require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const jwt = require('jsonwebtoken');

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);

const io = new Server(server, {
  cors: {
    origin: ['http://localhost:3000', 'http://localhost:8080'],
    methods: ['GET', 'POST'],
    credentials: true
  }
});

const JWT_SECRET = process.env.JWT_SECRET || 'default-secret-key';

const activeEditors = new Map();
const userSockets = new Map();
const documentRooms = new Map();

const documentStates = new Map();

class DocumentOperationBuffer {
  constructor() {
    this.pendingOperations = [];
    this.lastProcessedVersion = 0;
  }
  
  addOperation(operation, fromVersion, userId) {
    this.pendingOperations.push({
      operation,
      fromVersion,
      userId,
      timestamp: Date.now()
    });
  }
  
  getPendingOperations() {
    return [...this.pendingOperations];
  }
  
  clearOperations() {
    this.pendingOperations = [];
  }
}

class OTAlgorithm {
  
  static transform(opsA, opsB) {
    const resultA = [];
    const resultB = [];
    
    let i = 0, j = 0;
    
    while (i < opsA.length && j < opsB.length) {
      const a = opsA[i];
      const b = opsB[j];
      
      if (a.type === 'insert' && b.type === 'insert') {
        if (i <= j) {
          resultA.push(a);
          resultB.push({ type: 'retain', retainCount: a.insertText.length });
          i++;
        } else {
          resultA.push({ type: 'retain', retainCount: b.insertText.length });
          resultB.push(b);
          j++;
        }
      }
      else if (a.type === 'insert') {
        resultA.push(a);
        resultB.push({ type: 'retain', retainCount: a.insertText.length });
        i++;
      }
      else if (b.type === 'insert') {
        resultA.push({ type: 'retain', retainCount: b.insertText.length });
        resultB.push(b);
        j++;
      }
      else {
        const lenA = this.getBaseLength(a);
        const lenB = this.getBaseLength(b);
        
        if (lenA < lenB) {
          const newB = this.cloneOperation(b, lenA);
          resultA.push(a);
          resultB.push(newB);
          
          if (b.type === 'delete') {
            opsB[j] = { type: 'delete', deleteText: b.deleteText.substring(lenA) };
          } else if (b.type === 'retain') {
            opsB[j] = { type: 'retain', retainCount: b.retainCount - lenA };
          }
          i++;
        }
        else if (lenA > lenB) {
          const newA = this.cloneOperation(a, lenB);
          resultA.push(newA);
          resultB.push(b);
          
          if (a.type === 'delete') {
            opsA[i] = { type: 'delete', deleteText: a.deleteText.substring(lenB) };
          } else if (a.type === 'retain') {
            opsA[i] = { type: 'retain', retainCount: a.retainCount - lenB };
          }
          j++;
        }
        else {
          if (a.type === 'retain' && b.type === 'retain') {
            resultA.push({ type: 'retain', retainCount: lenA });
            resultB.push({ type: 'retain', retainCount: lenB });
          }
          else if (a.type === 'delete' && b.type === 'retain') {
            resultA.push(a);
          }
          else if (a.type === 'retain' && b.type === 'delete') {
            resultB.push(b);
          }
          i++;
          j++;
        }
      }
    }
    
    while (i < opsA.length) {
      const a = opsA[i];
      if (a.type === 'insert') {
        resultA.push(a);
        resultB.push({ type: 'retain', retainCount: a.insertText.length });
      } else {
        resultA.push(a);
      }
      i++;
    }
    
    while (j < opsB.length) {
      const b = opsB[j];
      if (b.type === 'insert') {
        resultA.push({ type: 'retain', retainCount: b.insertText.length });
        resultB.push(b);
      } else {
        resultB.push(b);
      }
      j++;
    }
    
    return { first: resultA, second: resultB };
  }
  
  static getBaseLength(op) {
    switch (op.type) {
      case 'retain': return op.retainCount;
      case 'insert': return 0;
      case 'delete': return op.deleteText.length;
      default: return 0;
    }
  }
  
  static cloneOperation(op, length) {
    switch (op.type) {
      case 'retain':
        return { type: 'retain', retainCount: length };
      case 'insert':
        return { type: 'insert', insertText: op.insertText.substring(0, length) };
      case 'delete':
        return { type: 'delete', deleteText: op.deleteText.substring(0, length) };
      default:
        return null;
    }
  }
  
  static apply(operations, document) {
    let result = '';
    let docIndex = 0;
    
    for (const op of operations) {
      switch (op.type) {
        case 'retain':
          if (docIndex + op.retainCount > document.length) {
            throw new Error('Retain operation exceeds document length');
          }
          result += document.substring(docIndex, docIndex + op.retainCount);
          docIndex += op.retainCount;
          break;
          
        case 'insert':
          result += op.insertText;
          break;
          
        case 'delete':
          const toDelete = document.substring(docIndex, docIndex + op.deleteText.length);
          if (toDelete !== op.deleteText) {
            console.warn('Delete operation text mismatch');
          }
          docIndex += op.deleteText.length;
          break;
      }
    }
    
    if (docIndex < document.length) {
      result += document.substring(docIndex);
    }
    
    return result;
  }
  
  static diff(oldStr, newStr) {
    const m = oldStr.length;
    const n = newStr.length;
    
    const dp = Array(m + 1).fill(null).map(() => Array(n + 1).fill(0));
    
    for (let i = 0; i <= m; i++) dp[i][0] = i;
    for (let j = 0; j <= n; j++) dp[0][j] = j;
    
    for (let i = 1; i <= m; i++) {
      for (let j = 1; j <= n; j++) {
        if (oldStr[i - 1] === newStr[j - 1]) {
          dp[i][j] = dp[i - 1][j - 1];
        } else {
          dp[i][j] = 1 + Math.min(dp[i - 1][j], dp[i][j - 1]);
        }
      }
    }
    
    const operations = [];
    let i = m, j = n;
    
    while (i > 0 || j > 0) {
      if (i > 0 && j > 0 && oldStr[i - 1] === newStr[j - 1]) {
        i--; j--;
        this.addRetainOrMerge(operations, 1);
      }
      else if (i > 0 && (j === 0 || dp[i - 1][j] <= dp[i][j - 1])) {
        this.addDeleteOrMerge(operations, oldStr[i - 1]);
        i--;
      }
      else {
        this.addInsertOrMerge(operations, newStr[j - 1]);
        j--;
      }
    }
    
    return operations.reverse();
  }
  
  static addRetainOrMerge(ops, count) {
    if (ops.length === 0 || ops[ops.length - 1].type !== 'retain') {
      ops.push({ type: 'retain', retainCount: count });
    } else {
      ops[ops.length - 1].retainCount += count;
    }
  }
  
  static addDeleteOrMerge(ops, ch) {
    if (ops.length === 0 || ops[ops.length - 1].type !== 'delete') {
      ops.push({ type: 'delete', deleteText: ch });
    } else {
      ops[ops.length - 1].deleteText = ch + ops[ops.length - 1].deleteText;
    }
  }
  
  static addInsertOrMerge(ops, ch) {
    if (ops.length === 0 || ops[ops.length - 1].type !== 'insert') {
      ops.push({ type: 'insert', insertText: ch });
    } else {
      ops[ops.length - 1].insertText = ch + ops[ops.length - 1].insertText;
    }
  }
}

function authenticateToken(socket, next) {
  const token = socket.handshake.auth.token || socket.handshake.headers.authorization?.split(' ')[1];
  
  if (!token) {
    return next(new Error('Authentication error: No token provided'));
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    socket.user = {
      id: decoded.sub,
      username: decoded.sub
    };
    next();
  } catch (err) {
    return next(new Error('Authentication error: Invalid token'));
  }
}

io.use(authenticateToken);

io.on('connection', (socket) => {
  console.log(`User connected: ${socket.user.username} (${socket.id})`);

  if (!userSockets.has(socket.user.id)) {
    userSockets.set(socket.user.id, new Set());
  }
  userSockets.get(socket.user.id).add(socket.id);

  socket.on('join-document', async ({ documentId, spaceId }) => {
    const roomId = `doc:${documentId}`;
    
    await socket.join(roomId);
    
    if (!documentRooms.has(documentId)) {
      documentRooms.set(documentId, new Set());
    }
    documentRooms.get(documentId).add(socket.user.id);

    if (!activeEditors.has(documentId)) {
      activeEditors.set(documentId, new Map());
    }
    
    const userInfo = {
      id: socket.user.id,
      username: socket.user.username,
      socketId: socket.id,
      joinedAt: Date.now()
    };
    activeEditors.get(documentId).set(socket.user.id, userInfo);

    const editors = Array.from(activeEditors.get(documentId).values());
    socket.to(roomId).emit('user-joined', userInfo);
    socket.emit('active-editors', editors);

    console.log(`User ${socket.user.username} joined document ${documentId}`);
  });

  socket.on('leave-document', async ({ documentId }) => {
    const roomId = `doc:${documentId}`;
    
    await socket.leave(roomId);

    if (activeEditors.has(documentId)) {
      activeEditors.get(documentId).delete(socket.user.id);
      
      if (activeEditors.get(documentId).size === 0) {
        activeEditors.delete(documentId);
      } else {
        socket.to(roomId).emit('user-left', {
          id: socket.user.id,
          username: socket.user.username
        });
      }
    }

    if (documentRooms.has(documentId)) {
      documentRooms.get(documentId).delete(socket.user.id);
      if (documentRooms.get(documentId).size === 0) {
        documentRooms.delete(documentId);
      }
    }

    console.log(`User ${socket.user.username} left document ${documentId}`);
  });

  socket.on('edit-content', ({ documentId, content, cursorPosition }) => {
    const roomId = `doc:${documentId}`;
    
    socket.to(roomId).emit('content-edited', {
      userId: socket.user.id,
      username: socket.user.username,
      content,
      cursorPosition,
      timestamp: Date.now()
    });
  });

  socket.on('cursor-move', ({ documentId, cursorPosition, selection }) => {
    const roomId = `doc:${documentId}`;
    
    socket.to(roomId).emit('cursor-moved', {
      userId: socket.user.id,
      username: socket.user.username,
      cursorPosition,
      selection,
      timestamp: Date.now()
    });
  });

  socket.on('auto-save', ({ documentId, draftContent }) => {
    const roomId = `doc:${documentId}`;
    
    socket.to(roomId).emit('auto-save-notification', {
      userId: socket.user.id,
      username: socket.user.username,
      timestamp: Date.now()
    });
  });

  socket.on('new-comment', ({ documentId, comment }) => {
    const roomId = `doc:${documentId}`;
    
    io.to(roomId).emit('comment-added', {
      userId: socket.user.id,
      username: socket.user.username,
      comment,
      timestamp: Date.now()
    });

    socket.to(roomId).emit('notification', {
      type: 'NEW_COMMENT',
      title: 'New Comment',
      message: `${socket.user.username} commented on this document`,
      comment
    });
  });

  socket.on('update-comment', ({ documentId, comment }) => {
    const roomId = `doc:${documentId}`;
    
    io.to(roomId).emit('comment-updated', {
      userId: socket.user.id,
      comment,
      timestamp: Date.now()
    });
  });

  socket.on('delete-comment', ({ documentId, commentId }) => {
    const roomId = `doc:${documentId}`;
    
    io.to(roomId).emit('comment-deleted', {
      userId: socket.user.id,
      commentId,
      timestamp: Date.now()
    });
  });

  socket.on('document-saved', ({ documentId, version }) => {
    const roomId = `doc:${documentId}`;
    
    io.to(roomId).emit('document-saved', {
      userId: socket.user.id,
      username: socket.user.username,
      version,
      timestamp: Date.now()
    });
  });

  socket.on('document-renamed', ({ documentId, newTitle }) => {
    const roomId = `doc:${documentId}`;
    
    io.to(roomId).emit('document-renamed', {
      userId: socket.user.id,
      documentId,
      newTitle,
      timestamp: Date.now()
    });
  });

  socket.on('send-message', ({ documentId, message, targetUserId }) => {
    const targetSockets = userSockets.get(targetUserId);
    if (targetSockets) {
      targetSockets.forEach(socketId => {
        io.to(socketId).emit('direct-message', {
          fromUserId: socket.user.id,
          fromUsername: socket.user.username,
          message,
          documentId,
          timestamp: Date.now()
        });
      });
    }
  });

  socket.on('typing-start', ({ documentId }) => {
    const roomId = `doc:${documentId}`;
    socket.to(roomId).emit('user-typing', {
      userId: socket.user.id,
      username: socket.user.username,
      isTyping: true
    });
  });

  socket.on('typing-stop', ({ documentId }) => {
    const roomId = `doc:${documentId}`;
    socket.to(roomId).emit('user-typing', {
      userId: socket.user.id,
      username: socket.user.username,
      isTyping: false
    });
  });

  socket.on('operation-submit', ({ documentId, operations, fromVersion, baseContent }) => {
    const roomId = `doc:${documentId}`;
    
    console.log(`[OT] User ${socket.user.username} submitted ${operations.length} operations for document ${documentId} from version ${fromVersion}`);
    
    if (!documentStates.has(documentId)) {
      documentStates.set(documentId, {
        content: baseContent || '',
        version: fromVersion || 1,
        pendingOps: []
      });
    }
    
    const docState = documentStates.get(documentId);
    
    if (fromVersion < docState.version) {
      console.log(`[OT] Need to transform operations: client version ${fromVersion} < server version ${docState.version}`);
      
      const concurrentOps = docState.pendingOps.filter(
        op => op.fromVersion >= fromVersion
      ).flatMap(op => op.operations);
      
      if (concurrentOps.length > 0) {
        console.log(`[OT] Found ${concurrentOps.length} concurrent operations to transform against`);
        
        try {
          let currentOps = [...operations];
          
          for (const concurrentOpGroup of docState.pendingOps) {
            if (concurrentOpGroup.fromVersion >= fromVersion) {
              const transformed = OTAlgorithm.transform(
                currentOps,
                concurrentOpGroup.operations
              );
              currentOps = transformed.first;
            }
          }
          
          operations = currentOps;
          console.log(`[OT] Operations transformed successfully`);
        } catch (error) {
          console.error(`[OT] Transform error:`, error);
          socket.emit('operation-error', {
            documentId,
            error: 'Operation transform failed',
            timestamp: Date.now()
          });
          return;
        }
      }
    }
    
    try {
      const newContent = OTAlgorithm.apply(operations, docState.content);
      const newVersion = docState.version + 1;
      
      docState.content = newContent;
      docState.version = newVersion;
      docState.pendingOps.push({
        operations,
        fromVersion,
        userId: socket.user.id,
        timestamp: Date.now()
      });
      
      if (docState.pendingOps.length > 100) {
        docState.pendingOps = docState.pendingOps.slice(-50);
      }
      
      console.log(`[OT] Document ${documentId} updated to version ${newVersion}`);
      
      socket.emit('operation-ack', {
        documentId,
        success: true,
        newVersion,
        newContent,
        operations,
        timestamp: Date.now()
      });
      
      socket.to(roomId).emit('operation-broadcast', {
        documentId,
        operations,
        fromVersion: docState.version - 1,
        newVersion,
        userId: socket.user.id,
        username: socket.user.username,
        timestamp: Date.now()
      });
      
      console.log(`[OT] Operations broadcasted to other users`);
      
    } catch (error) {
      console.error(`[OT] Apply error:`, error);
      socket.emit('operation-error', {
        documentId,
        error: error.message,
        currentContent: docState.content,
        currentVersion: docState.version,
        timestamp: Date.now()
      });
    }
  });

  socket.on('sync-request', ({ documentId }) => {
    const roomId = `doc:${documentId}`;
    
    console.log(`[Sync] User ${socket.user.username} requested sync for document ${documentId}`);
    
    const docState = documentStates.get(documentId);
    
    if (docState) {
      socket.emit('sync-response', {
        documentId,
        content: docState.content,
        version: docState.version,
        timestamp: Date.now()
      });
      console.log(`[Sync] Sent sync response for document ${documentId}, version ${docState.version}`);
    } else {
      socket.emit('sync-request-failed', {
        documentId,
        error: 'Document state not found',
        timestamp: Date.now()
      });
    }
  });

  socket.on('initialize-document-state', ({ documentId, content, version }) => {
    console.log(`[Init] Initializing document state for ${documentId}, version ${version}`);
    
    documentStates.set(documentId, {
      content: content || '',
      version: version || 1,
      pendingOps: []
    });
    
    socket.emit('document-state-initialized', {
      documentId,
      version: version || 1,
      timestamp: Date.now()
    });
  });

  socket.on('compute-diff', ({ oldContent, newContent }) => {
    try {
      const operations = OTAlgorithm.diff(oldContent, newContent);
      
      socket.emit('diff-result', {
        operations,
        oldLength: oldContent.length,
        newLength: newContent.length,
        timestamp: Date.now()
      });
      
      console.log(`[Diff] Computed ${operations.length} operations`);
    } catch (error) {
      console.error(`[Diff] Error:`, error);
      socket.emit('diff-error', {
        error: error.message,
        timestamp: Date.now()
      });
    }
  });

  socket.on('apply-diff', ({ content, operations }) => {
    try {
      const newContent = OTAlgorithm.apply(operations, content);
      
      socket.emit('apply-diff-result', {
        success: true,
        newContent,
        timestamp: Date.now()
      });
    } catch (error) {
      console.error(`[Apply Diff] Error:`, error);
      socket.emit('apply-diff-error', {
        success: false,
        error: error.message,
        timestamp: Date.now()
      });
    }
  });

  socket.on('disconnecting', () => {
    console.log(`User disconnecting: ${socket.user.username} (${socket.id})`);
    
    const rooms = socket.rooms;
    rooms.forEach(room => {
      if (room.startsWith('doc:')) {
        const documentId = room.replace('doc:', '');
        
        if (activeEditors.has(documentId)) {
          activeEditors.get(documentId).delete(socket.user.id);
          
          if (activeEditors.get(documentId).size === 0) {
            activeEditors.delete(documentId);
          } else {
            socket.to(room).emit('user-left', {
              id: socket.user.id,
              username: socket.user.username
            });
          }
        }
      }
    });
  });

  socket.on('disconnect', () => {
    console.log(`User disconnected: ${socket.user.username} (${socket.id})`);
    
    if (userSockets.has(socket.user.id)) {
      userSockets.get(socket.user.id).delete(socket.id);
      if (userSockets.get(socket.user.id).size === 0) {
        userSockets.delete(socket.user.id);
      }
    }
  });
});

app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    activeDocuments: activeEditors.size,
    totalConnections: io.engine.clientsCount
  });
});

app.get('/api/active-editors/:documentId', (req, res) => {
  const { documentId } = req.params;
  
  if (activeEditors.has(documentId)) {
    const editors = Array.from(activeEditors.get(documentId).values());
    res.json({
      documentId,
      count: editors.length,
      editors: editors.map(e => ({
        id: e.id,
        username: e.username,
        joinedAt: e.joinedAt
      }))
    });
  } else {
    res.json({
      documentId,
      count: 0,
      editors: []
    });
  }
});

const PORT = process.env.PORT || 3001;

server.listen(PORT, () => {
  console.log(`WebSocket server running on port ${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
});

module.exports = { io, activeEditors, userSockets };
