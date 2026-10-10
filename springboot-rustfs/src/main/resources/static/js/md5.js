/**
 * 纯 JS 增量 MD5 实现(基于 RFC 1321),零外部依赖。
 *
 * 浏览器端计算文件 MD5 供"闪传"使用:SubtleCrypto 不支持 MD5,大文件也无法整块读入内存后一次性计算,
 * 因此这里提供可增量累积字节的摘要对象,再由 Md5.of() 分段读取 Blob 流式喂入。
 *
 * 暴露为全局 window.Md5,同时兼容 CommonJS 以便 Node 侧做对照测试。
 */
(function (global) {
  'use strict';

  /* 每轮运算的左移位数,RFC 1321 §3.4 */
  var SHIFTS = [
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
  ];

  /* 常量表 K[i] = floor(abs(sin(i + 1)) * 2^32) */
  var K = new Int32Array(64);
  for (var k = 0; k < 64; k++) {
    K[k] = Math.floor(Math.abs(Math.sin(k + 1)) * 0x100000000) | 0;
  }

  /**
   * 增量 MD5 摘要
   * @constructor
   */
  function Md5() {
    this.a0 = 0x67452301;
    this.b0 = 0xefcdab89;
    this.c0 = 0x98badcfe;
    this.d0 = 0x10325476;
    /* 不足一个分组时暂存的字节 */
    this.buffer = new Uint8Array(64);
    this.bufferLength = 0;
    /* 已喂入的总字节数,用于填充长度字段 */
    this.totalLength = 0;
    /* 复用的字数组,避免每个分组都新建对象造成 GC 压力 */
    this.words = new Int32Array(16);
  }

  /**
   * 处理一个 64 字节分组
   * @param {Uint8Array} block 长度为 64 的分组
   * @private
   */
  Md5.prototype._cycle = function (block) {
    var a = this.a0, b = this.b0, c = this.c0, d = this.d0;
    var words = this.words, i, j;

    /* 按小端序把 64 字节解释为 16 个 32 位字 */
    for (i = 0; i < 16; i++) {
      j = i << 2;
      words[i] = block[j] | (block[j + 1] << 8) | (block[j + 2] << 16) | (block[j + 3] << 24);
    }

    for (i = 0; i < 64; i++) {
      var f, g;
      if (i < 16) {
        f = (b & c) | (~b & d);
        g = i;
      } else if (i < 32) {
        f = (d & b) | (~d & c);
        g = (5 * i + 1) % 16;
      } else if (i < 48) {
        f = b ^ c ^ d;
        g = (3 * i + 5) % 16;
      } else {
        f = c ^ (b | ~d);
        g = (7 * i) % 16;
      }
      f = (f + a + K[i] + words[g]) | 0;
      a = d;
      d = c;
      c = b;
      b = (b + ((f << SHIFTS[i]) | (f >>> (32 - SHIFTS[i])))) | 0;
    }

    this.a0 = (this.a0 + a) | 0;
    this.b0 = (this.b0 + b) | 0;
    this.c0 = (this.c0 + c) | 0;
    this.d0 = (this.d0 + d) | 0;
  };

  /**
   * 追加字节,可分多次调用,中间不产生中间摘要
   * @param {Uint8Array} bytes
   * @returns {Md5} this
   */
  Md5.prototype.append = function (bytes) {
    var length = bytes.length;
    this.totalLength += length;
    var offset = 0;
    var buffer = this.buffer;
    var buffered = this.bufferLength;

    /* 上次残留的字节凑满一个分组 */
    if (buffered > 0) {
      var need = 64 - buffered;
      if (length < need) {
        buffer.set(bytes.subarray(0, length), buffered);
        this.bufferLength = buffered + length;
        return this;
      }
      buffer.set(bytes.subarray(0, need), buffered);
      this._cycle(buffer);
      offset = need;
      this.bufferLength = 0;
    }

    /* 完整分组逐个处理,不做复制 */
    while (offset + 64 <= length) {
      this._cycle(bytes.subarray(offset, offset + 64));
      offset += 64;
    }

    /* 尾巴留给下次 */
    if (offset < length) {
      buffer.set(bytes.subarray(offset), 0);
      this.bufferLength = length - offset;
    }
    return this;
  };

  /**
   * 结束并输出小写 hex 摘要;调用后对象状态不可再用于追加
   * @returns {string} 32 位小写 hex
   */
  Md5.prototype.digest = function () {
    /* 先取位长度:padding 追加的字节不能算进去 */
    var bitLength = this.totalLength * 8;
    var low = bitLength >>> 0;
    var high = Math.floor(bitLength / 0x100000000) >>> 0;

    /* 填充:0x80 之后补 0,直到长度 ≡ 56 (mod 64),末尾 8 字节写位长度(小端) */
    var padLength = this.bufferLength < 56 ? 56 - this.bufferLength : 120 - this.bufferLength;
    var padding = new Uint8Array(padLength + 8);
    padding[0] = 0x80;
    padding[padLength] = low & 0xff;
    padding[padLength + 1] = (low >>> 8) & 0xff;
    padding[padLength + 2] = (low >>> 16) & 0xff;
    padding[padLength + 3] = (low >>> 24) & 0xff;
    padding[padLength + 4] = high & 0xff;
    padding[padLength + 5] = (high >>> 8) & 0xff;
    padding[padLength + 6] = (high >>> 16) & 0xff;
    padding[padLength + 7] = (high >>> 24) & 0xff;
    this.append(padding);

    var words = [this.a0, this.b0, this.c0, this.d0];
    var hex = '', i, j;
    for (i = 0; i < 4; i++) {
      for (j = 0; j < 4; j++) {
        hex += ('0' + ((words[i] >>> (j * 8)) & 0xff).toString(16)).slice(-2);
      }
    }
    return hex;
  };

  /**
   * 一次性计算 Uint8Array 的 MD5
   * @param {Uint8Array} bytes
   * @returns {string}
   */
  Md5.ofBytes = function (bytes) {
    return new Md5().append(bytes).digest();
  };

  /**
   * 分段读取 Blob 并流式计算 MD5,不会把整个文件读进内存
   * @param {Blob} blob File/Blob
   * @param {Object} [options]
   * @param {number} [options.sliceSize=4MB] 单次读取的字节数
   * @param {function(number, number)} [options.onProgress] 回调(已读字节, 总字节)
   * @returns {Promise<string>} 小写 hex
   */
  Md5.of = function (blob, options) {
    var opts = options || {};
    var sliceSize = opts.sliceSize || 4 * 1024 * 1024;
    var onProgress = opts.onProgress;

    return new Promise(function (resolve, reject) {
      if (!blob || typeof blob.slice !== 'function') {
        reject(new Error('需要 Blob 或 File 对象'));
        return;
      }

      var total = blob.size;
      var md5 = new Md5();
      var offset = 0;

      function step() {
        if (offset >= total) {
          if (onProgress) { onProgress(total, total); }
          resolve(md5.digest());
          return;
        }
        var end = Math.min(offset + sliceSize, total);
        readBytes(blob.slice(offset, end)).then(function (bytes) {
          md5.append(bytes);
          offset = end;
          if (onProgress) { onProgress(offset, total); }
          step();
        }).catch(reject);
      }

      step();
    });
  };

  /**
   * Blob -> Uint8Array,优先用 arrayBuffer(),回退到 FileReader
   * @param {Blob} blob
   * @returns {Promise<Uint8Array>}
   */
  function readBytes(blob) {
    if (typeof blob.arrayBuffer === 'function') {
      return blob.arrayBuffer().then(function (buffer) {
        return new Uint8Array(buffer);
      });
    }
    return new Promise(function (resolve, reject) {
      var reader = new FileReader();
      reader.onload = function () { resolve(new Uint8Array(reader.result)); };
      reader.onerror = function () { reject(reader.error || new Error('读取文件失败')); };
      reader.readAsArrayBuffer(blob);
    });
  }

  var api = { Md5: Md5, of: Md5.of, ofBytes: Md5.ofBytes };

  if (typeof module === 'object' && module.exports) {
    module.exports = api;
  }
  if (global) {
    global.Md5 = api;
  }
})(typeof window !== 'undefined' ? window : (typeof globalThis !== 'undefined' ? globalThis : this));
