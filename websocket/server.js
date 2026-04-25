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
