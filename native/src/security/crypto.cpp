#include "nebula/security/crypto.hpp"
#include <cstring>
#include <algorithm>

namespace nebula {
namespace security {

void secureWipe(void* ptr, size_t size) {
    if (!ptr || size == 0) return;
    volatile uint8_t* p = static_cast<volatile uint8_t*>(ptr);
    while (size--) {
        *p++ = 0;
    }
}

namespace {

inline uint32_t rotr(uint32_t x, uint32_t n) {
    return (x >> n) | (x << (32 - n));
}

inline uint32_t ch(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (~x & z);
}

inline uint32_t maj(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (x & z) ^ (y & z);
}

inline uint32_t sigma0(uint32_t x) {
    return rotr(x, 2) ^ rotr(x, 13) ^ rotr(x, 22);
}

inline uint32_t sigma1(uint32_t x) {
    return rotr(x, 6) ^ rotr(x, 11) ^ rotr(x, 25);
}

inline uint32_t gamma0(uint32_t x) {
    return rotr(x, 7) ^ rotr(x, 18) ^ (x >> 3);
}

inline uint32_t gamma1(uint32_t x) {
    return rotr(x, 17) ^ rotr(x, 19) ^ (x >> 10);
}

static const uint32_t K[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
};

} 

void sha256(const uint8_t* data, size_t len, uint8_t hashOut[32]) {
    uint32_t state[8] = {
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
        0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };

    uint64_t bitLen = len * 8;
    std::vector<uint8_t> padded(data, data + len);
    padded.push_back(0x80);
    while ((padded.size() % 64) != 56) {
        padded.push_back(0x00);
    }
    for (int i = 7; i >= 0; --i) {
        padded.push_back(static_cast<uint8_t>((bitLen >> (i * 8)) & 0xff));
    }

    for (size_t chunk = 0; chunk < padded.size(); chunk += 64) {
        uint32_t W[64];
        for (int i = 0; i < 16; ++i) {
            W[i] = (static_cast<uint32_t>(padded[chunk + i * 4]) << 24) |
                   (static_cast<uint32_t>(padded[chunk + i * 4 + 1]) << 16) |
                   (static_cast<uint32_t>(padded[chunk + i * 4 + 2]) << 8) |
                   (static_cast<uint32_t>(padded[chunk + i * 4 + 3]));
        }
        for (int i = 16; i < 64; ++i) {
            W[i] = gamma1(W[i - 2]) + W[i - 7] + gamma0(W[i - 15]) + W[i - 16];
        }

        uint32_t a = state[0], b = state[1], c = state[2], d = state[3];
        uint32_t e = state[4], f = state[5], g = state[6], h = state[7];

        for (int i = 0; i < 64; ++i) {
            uint32_t T1 = h + sigma1(e) + ch(e, f, g) + K[i] + W[i];
            uint32_t T2 = sigma0(a) + maj(a, b, c);
            h = g;
            g = f;
            f = e;
            e = d + T1;
            d = c;
            c = b;
            b = a;
            a = T1 + T2;
        }

        state[0] += a; state[1] += b; state[2] += c; state[3] += d;
        state[4] += e; state[5] += f; state[6] += g; state[7] += h;
    }

    for (int i = 0; i < 8; ++i) {
        hashOut[i * 4 + 0] = static_cast<uint8_t>((state[i] >> 24) & 0xff);
        hashOut[i * 4 + 1] = static_cast<uint8_t>((state[i] >> 16) & 0xff);
        hashOut[i * 4 + 2] = static_cast<uint8_t>((state[i] >> 8) & 0xff);
        hashOut[i * 4 + 3] = static_cast<uint8_t>(state[i] & 0xff);
    }

    secureWipe(padded.data(), padded.size());
}

namespace {

inline uint32_t rotl32(uint32_t v, int c) {
    return (v << c) | (v >> (32 - c));
}

inline void quarterRound(uint32_t& a, uint32_t& b, uint32_t& c, uint32_t& d) {
    a += b; d ^= a; d = rotl32(d, 16);
    c += d; b ^= c; b = rotl32(b, 12);
    a += b; d ^= a; d = rotl32(d, 8);
    c += d; b ^= c; b = rotl32(b, 7);
}

inline uint32_t load32Le(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) |
          (static_cast<uint32_t>(p[1]) << 8) |
          (static_cast<uint32_t>(p[2]) << 16) |
          (static_cast<uint32_t>(p[3]) << 24);
}

inline void store32Le(uint8_t* p, uint32_t v) {
    p[0] = static_cast<uint8_t>(v);
    p[1] = static_cast<uint8_t>(v >> 8);
    p[2] = static_cast<uint8_t>(v >> 16);
    p[3] = static_cast<uint8_t>(v >> 24);
}

} 

void chacha20Xor(const uint8_t key[32], const uint8_t nonce[12], uint32_t counter,
                 const uint8_t* input, uint8_t* output, size_t length) {
    uint32_t state[16];
    state[0] = 0x61707865;
    state[1] = 0x3320646e;
    state[2] = 0x79622d32;
    state[3] = 0x6b206574;

    for (int i = 0; i < 8; ++i) {
        state[4 + i] = load32Le(key + i * 4);
    }
    state[12] = counter;
    for (int i = 0; i < 3; ++i) {
        state[13 + i] = load32Le(nonce + i * 4);
    }

    uint8_t keyStream[64];
    size_t offset = 0;

    while (offset < length) {
        uint32_t work[16];
        memcpy(work, state, sizeof(state));

        for (int i = 0; i < 10; ++i) {
            quarterRound(work[0], work[4], work[8],  work[12]);
            quarterRound(work[1], work[5], work[9],  work[13]);
            quarterRound(work[2], work[6], work[10], work[14]);
            quarterRound(work[3], work[7], work[11], work[15]);
            quarterRound(work[0], work[5], work[10], work[15]);
            quarterRound(work[1], work[6], work[11], work[12]);
            quarterRound(work[2], work[7], work[8],  work[13]);
            quarterRound(work[3], work[4], work[9],  work[14]);
        }

        for (int i = 0; i < 16; ++i) {
            store32Le(keyStream + i * 4, work[i] + state[i]);
        }

        size_t take = std::min<size_t>(64, length - offset);
        for (size_t i = 0; i < take; ++i) {
            output[offset + i] = input[offset + i] ^ keyStream[i];
        }

        offset += take;
        state[12]++; 
    }

    secureWipe(keyStream, sizeof(keyStream));
}

bool decryptPayload(const uint8_t* rawData, size_t rawSize,
                    const uint8_t key[32],
                    std::vector<uint8_t>& decryptedOut) {
    if (rawSize < sizeof(PayloadHeader)) return false;

    const PayloadHeader* hdr = reinterpret_cast<const PayloadHeader*>(rawData);
    if (hdr->magic != 0x52415453) return false;
    if (hdr->version != 1) return false;

    const uint8_t* ciphertext = rawData + sizeof(PayloadHeader);
    size_t cipherLen = rawSize - sizeof(PayloadHeader);
    if (cipherLen != hdr->plainLength) return false;

    decryptedOut.resize(hdr->plainLength);
    chacha20Xor(key, hdr->nonce, 1, ciphertext, decryptedOut.data(), hdr->plainLength);

    uint8_t computedHash[32];
    sha256(decryptedOut.data(), decryptedOut.size(), computedHash);

    if (memcmp(computedHash, hdr->sha256, 32) != 0) {
        secureWipe(decryptedOut.data(), decryptedOut.size());
        decryptedOut.clear();
        return false;
    }

    return true;
}

} 
} 

