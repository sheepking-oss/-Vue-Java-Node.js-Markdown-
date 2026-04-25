export const OperationType = {
  RETAIN: 'retain',
  INSERT: 'insert',
  DELETE: 'delete'
}

export class Operation {
  constructor(type, retainCount = 0, insertText = '', deleteText = '') {
    this.type = type
    this.retainCount = retainCount
    this.insertText = insertText
    this.deleteText = deleteText
  }

  static retain(count) {
    return new Operation(OperationType.RETAIN, count)
  }

  static insert(text) {
    return new Operation(OperationType.INSERT, 0, text)
  }

  static delete(text) {
    return new Operation(OperationType.DELETE, 0, '', text)
  }

  getBaseLength() {
    switch (this.type) {
      case OperationType.RETAIN:
        return this.retainCount
      case OperationType.INSERT:
        return 0
      case OperationType.DELETE:
        return this.deleteText.length
      default:
        return 0
    }
  }

  getTargetLength() {
    switch (this.type) {
      case OperationType.RETAIN:
        return this.retainCount
      case OperationType.INSERT:
        return this.insertText.length
      case OperationType.DELETE:
        return 0
      default:
        return 0
    }
  }

  length() {
    switch (this.type) {
      case OperationType.RETAIN:
        return this.retainCount
      case OperationType.INSERT:
        return this.insertText.length
      case OperationType.DELETE:
        return this.deleteText.length
      default:
        return 0
    }
  }

  invert() {
    switch (this.type) {
      case OperationType.RETAIN:
        return Operation.retain(this.retainCount)
      case OperationType.INSERT:
        return Operation.delete(this.insertText)
      case OperationType.DELETE:
        return Operation.insert(this.deleteText)
      default:
        return null
    }
  }

  clone() {
    return new Operation(
      this.type,
      this.retainCount,
      this.insertText,
      this.deleteText
    )
  }

  toJSON() {
    return {
      type: this.type,
      retainCount: this.retainCount,
      insertText: this.insertText,
      deleteText: this.deleteText
    }
  }

  static fromJSON(json) {
    return new Operation(
      json.type,
      json.retainCount || 0,
      json.insertText || '',
      json.deleteText || ''
    )
  }

  toString() {
    switch (this.type) {
      case OperationType.RETAIN:
        return `retain(${this.retainCount})`
      case OperationType.INSERT:
        return `insert("${this.insertText}")`
      case OperationType.DELETE:
        return `delete("${this.deleteText}")`
      default:
        return 'unknown'
    }
  }
}

export class OTAlgorithm {
  
  static transform(opsA, opsB) {
    const resultA = []
    const resultB = []

    let i = 0, j = 0

    while (i < opsA.length && j < opsB.length) {
      const a = opsA[i]
      const b = opsB[j]

      if (a.type === OperationType.INSERT && b.type === OperationType.INSERT) {
        if (i <= j) {
          resultA.push(a)
          resultB.push(Operation.retain(a.insertText.length))
          i++
        } else {
          resultA.push(Operation.retain(b.insertText.length))
          resultB.push(b)
          j++
        }
      }
      else if (a.type === OperationType.INSERT) {
        resultA.push(a)
        resultB.push(Operation.retain(a.insertText.length))
        i++
      }
      else if (b.type === OperationType.INSERT) {
        resultA.push(Operation.retain(b.insertText.length))
        resultB.push(b)
        j++
      }
      else {
        const lenA = a.getBaseLength()
        const lenB = b.getBaseLength()

        if (lenA < lenB) {
          const newB = this.cloneOperation(b, lenA)
          resultA.push(a)
          resultB.push(newB)

          if (b.type === OperationType.DELETE) {
            opsB[j] = Operation.delete(b.deleteText.substring(lenA))
          } else if (b.type === OperationType.RETAIN) {
            opsB[j] = Operation.retain(b.retainCount - lenA)
          }
          i++
        }
        else if (lenA > lenB) {
          const newA = this.cloneOperation(a, lenB)
          resultA.push(newA)
          resultB.push(b)

          if (a.type === OperationType.DELETE) {
            opsA[i] = Operation.delete(a.deleteText.substring(lenB))
          } else if (a.type === OperationType.RETAIN) {
            opsA[i] = Operation.retain(a.retainCount - lenB)
          }
          j++
        }
        else {
          if (a.type === OperationType.RETAIN && b.type === OperationType.RETAIN) {
            resultA.push(Operation.retain(lenA))
            resultB.push(Operation.retain(lenB))
          }
          else if (a.type === OperationType.DELETE && b.type === OperationType.RETAIN) {
            resultA.push(a)
          }
          else if (a.type === OperationType.RETAIN && b.type === OperationType.DELETE) {
            resultB.push(b)
          }
          i++
          j++
        }
      }
    }

    while (i < opsA.length) {
      const a = opsA[i]
      if (a.type === OperationType.INSERT) {
        resultA.push(a)
        resultB.push(Operation.retain(a.insertText.length))
      } else {
        resultA.push(a)
      }
      i++
    }

    while (j < opsB.length) {
      const b = opsB[j]
      if (b.type === OperationType.INSERT) {
        resultA.push(Operation.retain(b.insertText.length))
        resultB.push(b)
      } else {
        resultB.push(b)
      }
      j++
    }

    return { first: resultA, second: resultB }
  }

  static cloneOperation(op, length) {
    switch (op.type) {
      case OperationType.RETAIN:
        return Operation.retain(length)
      case OperationType.INSERT:
        return Operation.insert(op.insertText.substring(0, length))
      case OperationType.DELETE:
        return Operation.delete(op.deleteText.substring(0, length))
      default:
        return null
    }
  }

  static apply(operations, document) {
    let result = ''
    let docIndex = 0

    for (const op of operations) {
      switch (op.type) {
        case OperationType.RETAIN:
          if (docIndex + op.retainCount > document.length) {
            throw new Error('Retain operation exceeds document length')
          }
          result += document.substring(docIndex, docIndex + op.retainCount)
          docIndex += op.retainCount
          break

        case OperationType.INSERT:
          result += op.insertText
          break

        case OperationType.DELETE:
          const toDelete = document.substring(docIndex, docIndex + op.deleteText.length)
          if (toDelete !== op.deleteText) {
            console.warn('Delete operation text mismatch')
          }
          docIndex += op.deleteText.length
          break
      }
    }

    if (docIndex < document.length) {
      result += document.substring(docIndex)
    }

    return result
  }

  static diff(oldStr, newStr) {
    const m = oldStr.length
    const n = newStr.length

    const dp = Array(m + 1).fill(null).map(() => Array(n + 1).fill(0))

    for (let i = 0; i <= m; i++) dp[i][0] = i
    for (let j = 0; j <= n; j++) dp[0][j] = j

    for (let i = 1; i <= m; i++) {
      for (let j = 1; j <= n; j++) {
        if (oldStr[i - 1] === newStr[j - 1]) {
          dp[i][j] = dp[i - 1][j - 1]
        } else {
          dp[i][j] = 1 + Math.min(dp[i - 1][j], dp[i][j - 1])
        }
      }
    }

    const operations = []
    let i = m, j = n

    while (i > 0 || j > 0) {
      if (i > 0 && j > 0 && oldStr[i - 1] === newStr[j - 1]) {
        i--
        j--
        this.addRetainOrMerge(operations, 1)
      }
      else if (i > 0 && (j === 0 || dp[i - 1][j] <= dp[i][j - 1])) {
        this.addDeleteOrMerge(operations, oldStr[i - 1])
        i--
      }
      else {
        this.addInsertOrMerge(operations, newStr[j - 1])
        j--
      }
    }

    return operations.reverse()
  }

  static addRetainOrMerge(ops, count) {
    if (ops.length === 0 || ops[ops.length - 1].type !== OperationType.RETAIN) {
      ops.push(Operation.retain(count))
    } else {
      ops[ops.length - 1].retainCount += count
    }
  }

  static addDeleteOrMerge(ops, ch) {
    if (ops.length === 0 || ops[ops.length - 1].type !== OperationType.DELETE) {
      ops.push(Operation.delete(ch))
    } else {
      ops[ops.length - 1].deleteText = ch + ops[ops.length - 1].deleteText
    }
  }

  static addInsertOrMerge(ops, ch) {
    if (ops.length === 0 || ops[ops.length - 1].type !== OperationType.INSERT) {
      ops.push(Operation.insert(ch))
    } else {
      ops[ops.length - 1].insertText = ch + ops[ops.length - 1].insertText
    }
  }

  static compose(ops1, ops2) {
    const result = []

    let i = 0, j = 0

    while (i < ops1.length || j < ops2.length) {
      const op1 = i < ops1.length ? ops1[i] : null
      const op2 = j < ops2.length ? ops2[j] : null

      if (!op1) {
        result.push(op2)
        j++
      }
      else if (!op2) {
        result.push(op1)
        i++
      }
      else if (op1.type === OperationType.INSERT) {
        result.push(op1)
        i++
      }
      else if (op2.type === OperationType.DELETE) {
        result.push(op2)
        j++
      }
      else {
        const len1 = op1.type === OperationType.RETAIN ? op1.retainCount : op1.deleteText.length
        const len2 = op2.type === OperationType.RETAIN ? op2.retainCount : op2.insertText.length

        if (len1 < len2) {
          if (op1.type === OperationType.DELETE) {
            result.push(op1)
          }

          if (op2.type === OperationType.INSERT) {
            result.push(Operation.insert(op2.insertText.substring(0, len1)))
            ops2[j] = Operation.insert(op2.insertText.substring(len1))
          }
          else if (op2.type === OperationType.RETAIN) {
            if (op1.type !== OperationType.DELETE) {
              result.push(Operation.retain(len1))
            }
            ops2[j] = Operation.retain(op2.retainCount - len1)
          }
          i++
        }
        else if (len1 > len2) {
          if (op1.type === OperationType.DELETE) {
            result.push(Operation.delete(op1.deleteText.substring(0, len2)))
            ops1[i] = Operation.delete(op1.deleteText.substring(len2))
          }
          else if (op1.type === OperationType.RETAIN) {
            if (op2.type === OperationType.INSERT) {
              result.push(op2)
            }
            else {
              result.push(Operation.retain(len2))
            }
            ops1[i] = Operation.retain(op1.retainCount - len2)
          }
          j++
        }
        else {
          if (op1.type === OperationType.DELETE) {
            result.push(op1)
          }
          else if (op2.type === OperationType.INSERT) {
            result.push(op2)
          }
          else if (op1.type === OperationType.RETAIN && op2.type === OperationType.RETAIN) {
            result.push(Operation.retain(len1))
          }
          i++
          j++
        }
      }
    }

    return result
  }

  static invert(operations) {
    const inverted = []
    for (let i = operations.length - 1; i >= 0; i--) {
      inverted.push(operations[i].invert())
    }
    return inverted
  }
}

export function computeDiff(oldContent, newContent) {
  return OTAlgorithm.diff(oldContent, newContent).map(op => op.toJSON())
}

export function applyOperations(content, operations) {
  const ops = operations.map(op => Operation.fromJSON(op))
  return OTAlgorithm.apply(ops, content)
}

export function transformOperations(opsA, opsB) {
  const a = opsA.map(op => Operation.fromJSON(op))
  const b = opsB.map(op => Operation.fromJSON(op))
  const result = OTAlgorithm.transform(a, b)
  return {
    first: result.first.map(op => op.toJSON()),
    second: result.second.map(op => op.toJSON())
  }
}

export default {
  Operation,
  OperationType,
  OTAlgorithm,
  computeDiff,
  applyOperations,
  transformOperations
}
