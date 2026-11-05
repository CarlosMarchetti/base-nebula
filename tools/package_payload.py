# Python / package_payload.py / Python 3 / compatible Nebula native payload
import argparse
import hashlib
from pathlib import Path
import struct
import zipfile

MASTER_KEY = b"NebulaBase-Client-Key-2026-Lunar"

def rotl32(v, c):
    return ((v << c) & 0xffffffff) | (v >> (32 - c))


def qround(work, a, b, c, d):
    work[a] = (work[a] + work[b]) & 0xffffffff
    work[d] ^= work[a]; work[d] = rotl32(work[d], 16)
    work[c] = (work[c] + work[d]) & 0xffffffff
    work[b] ^= work[c]; work[b] = rotl32(work[b], 12)
    work[a] = (work[a] + work[b]) & 0xffffffff
    work[d] ^= work[a]; work[d] = rotl32(work[d], 8)
    work[c] = (work[c] + work[d]) & 0xffffffff
    work[b] ^= work[c]; work[b] = rotl32(work[b], 7)


def chacha20_block(key, counter, nonce):
    state = [
        0x61707865, 0x3320646e, 0x79622d32, 0x6b206574,
        *struct.unpack('<8I', key),
        counter,
        *struct.unpack('<3I', nonce)
    ]
    work = list(state)
    for _ in range(10):
        qround(work, 0, 4, 8, 12); qround(work, 1, 5, 9, 13)
        qround(work, 2, 6, 10, 14); qround(work, 3, 7, 11, 15)
        qround(work, 0, 5, 10, 15); qround(work, 1, 6, 11, 12)
        qround(work, 2, 7, 8, 13); qround(work, 3, 4, 9, 14)
    return b''.join(struct.pack('<I', (work[i] + state[i]) & 0xffffffff) for i in range(16))


def chacha20_xor(key, nonce, counter, data):
    out = bytearray(len(data))
    offset = 0
    while offset < len(data):
        ks = chacha20_block(key, counter, nonce)
        take = min(64, len(data) - offset)
        for i in range(take):
            out[offset + i] = data[offset + i] ^ ks[i]
        offset += take
        counter = (counter + 1) & 0xffffffff
    return bytes(out)


def pack_payload(jar_path, blob_path):
    entries = []
    with zipfile.ZipFile(jar_path) as jar:
        names = jar.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Duplicate JAR entries")
        if "mappings/mcp-1.8.9.json" not in names:
            raise ValueError("Canonical mappings missing from JAR")
        if any(name.startswith("org/objectweb/asm/") for name in names):
            raise ValueError("ASM must be relocated into the client namespace")
        for info in jar.infolist():
            if info.is_dir() or info.filename.startswith("META-INF/") or info.filename == "module-info.class":
                continue
            entries.append((info.filename, jar.read(info.filename)))

    # Serializa os dados brutos
    raw_stream = bytearray()
    raw_stream.extend(struct.pack("<I", len(entries)))
    for name, data in entries:
        raw_name = name.encode("utf-8")
        raw_stream.extend(struct.pack("<I", len(raw_name)) + raw_name)
        raw_stream.extend(struct.pack("<I", len(data)) + data)

    plain_bytes = bytes(raw_stream)
    sha256_hash = hashlib.sha256(plain_bytes).digest()

    # Nonce determinístico baseado no hash para consistência de build
    nonce = hashlib.sha256(plain_bytes + b"star_nonce").digest()[:12]

    # Criptografa o stream com ChaCha20
    ciphertext = chacha20_xor(MASTER_KEY, nonce, 1, plain_bytes)

    # Monta cabeçalho do payload protegido
    # uint32 magic ('STAR' = 0x52415453), uint16 version (1), uint16 flags (1), nonce (12B), plainLen (4B), sha256 (32B)
    magic = b"STAR"
    header = struct.pack("<4sHH12sI32s", magic, 1, 1, nonce, len(plain_bytes), sha256_hash)

    with open(blob_path, "wb") as f:
        f.write(header)
        f.write(ciphertext)

    print(f"  {len(entries)} entradas protegidas com ChaCha20 + SHA-256 no payload ({len(header) + len(ciphertext)} bytes)")


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--jar',required=True)
    parser.add_argument('--blob',required=True)
    parser.add_argument('--asm',required=True)
    args=parser.parse_args()
    blob=Path(args.blob).resolve();blob.parent.mkdir(parents=True,exist_ok=True)
    pack_payload(args.jar,str(blob))
    assembly=Path(args.asm);assembly.parent.mkdir(parents=True,exist_ok=True)
    assembly.write_text('.section .rdata,"dr"\n.global g_payload\ng_payload:\n.incbin "'+blob.as_posix()+'"\n.global g_payload_end\ng_payload_end:\n.byte 0\n',encoding='ascii')
if __name__=='__main__':main()
