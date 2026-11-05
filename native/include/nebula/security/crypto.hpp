#pragma once

#include <cstdint>
#include <cstddef>
#include <vector>
#include <string>

namespace nebula {
namespace security {

void secureWipe(void* ptr, size_t size);

void sha256(const uint8_t* data, size_t len, uint8_t hashOut[32]);

void chacha20Xor(const uint8_t key[32], const uint8_t nonce[12], uint32_t counter,
                 const uint8_t* input, uint8_t* output, size_t length);

#pragma pack(push, 1)
struct PayloadHeader {
    uint32_t magic;         
    uint16_t version;       
    uint16_t flags;         
    uint8_t  nonce[12];     
    uint32_t plainLength;   
    uint8_t  sha256[32];    
};
#pragma pack(pop)

bool decryptPayload(const uint8_t* rawData, size_t rawSize,
                    const uint8_t key[32],
                    std::vector<uint8_t>& decryptedOut);

} 
} 

