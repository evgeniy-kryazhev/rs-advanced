"""Generate the empty structure used by both loaders' integration tests."""

import gzip
from pathlib import Path
import struct


def nbt_string(value: str) -> bytes:
    encoded_value = value.encode("utf-8")
    return struct.pack(">H", len(encoded_value)) + encoded_value


def named_tag(tag_type: int, name: str, payload: bytes) -> bytes:
    return bytes([tag_type]) + nbt_string(name) + payload


def main() -> None:
    # Empty block and entity lists leave the 8x3x8 test volume unobstructed.
    structure = named_tag(3, "DataVersion", struct.pack(">i", 3955))
    structure += named_tag(9, "size", bytes([3]) + struct.pack(">iiii", 3, 8, 3, 8))
    structure += named_tag(9, "palette", bytes([10]) + struct.pack(">i", 1)
                           + named_tag(8, "Name", nbt_string("minecraft:air")) + bytes([0]))
    structure += named_tag(9, "blocks", bytes([10]) + struct.pack(">i", 0))
    structure += named_tag(9, "entities", bytes([10]) + struct.pack(">i", 0))
    root_compound = bytes([10]) + nbt_string("") + structure + bytes([0])

    project_root = Path(__file__).resolve().parent.parent
    destination = project_root / "common/src/gameTest/resources/data/rsadvanced_test/structure/empty.nbt"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(gzip.compress(root_compound, mtime=0))


if __name__ == "__main__":
    main()
