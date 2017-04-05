/*
 * Copyright 2017-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.android.resources;

import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;

/**
 * ResTableType is a ResChunk holding the resource values for a given type and configuration.
 * It consists of:
 *   ResChunk_header
 *      u32 chunk_type
 *      u32 header_size
 *      u32 chunk_size
 *   u8  id
 *   u32 entry_count
 *   u32 entry_start
 *   Config
 *      u32 config_size
 *      u8[config_size - 4] data
 *
 * This is followed by entry_count u32s containing offsets from entry_start to the data for each
 * entry. If the offset for a resource is -1, that resource has no value in this configuration.
 *
 * After the offsets comes the entry data. Each entry is of the form:
 *   u16       size
 *   u16       flags
 *   StringRef key
 *
 * If `(flags & FLAG_COMPLEX) == 0` this data is then followed by:
 *   ResValue value
 *
 * Else it's followed by a map header:
 *   ResRef parent
 *   u32    count
 *
 * and `count` map entries of the form:
 *   ResRef   name
 *   ResValue value
 */
public class ResTableType extends ResChunk {
  private static final int CONFIG_OFFSET = 20;

  private final int id;
  private final int entryCount;
  private final ByteBuffer config;
  private final ByteBuffer entryOffsets;
  private final ByteBuffer entryData;

  @Override
  public void put(ByteBuffer output) {
    Preconditions.checkState(output.remaining() >= getChunkSize());
    int start = output.position();
    putChunkHeader(output);
    output.put((byte) (id + 1));
    output.put((byte) 0);
    output.putShort((byte) 0);
    output.putInt(entryCount);
    output.putInt(getHeaderSize() + 4 * entryCount);
    output.put(slice(config, 0));
    output.put(slice(entryOffsets, 0));
    output.put(slice(entryData, 0));
    Preconditions.checkState(output.position() == start + getChunkSize());
  }


  public static ResTableType get(ByteBuffer buf) {
    int type = buf.getShort();
    int headerSize = buf.getShort();
    int chunkSize = buf.getInt();
    int id = (buf.get() & 0xFF) - 1;
    buf.get(); // ignored
    buf.getShort(); // ignored
    int entryCount = buf.getInt();
    int entriesOffset = buf.getInt();
    int configSize = buf.getInt(buf.position());
    int entryDataSize = chunkSize - headerSize - 4 * entryCount;

    Preconditions.checkState(type == CHUNK_RES_TABLE_TYPE);
    Preconditions.checkState(headerSize == configSize + CONFIG_OFFSET);

    return new ResTableType(
        headerSize,
        chunkSize,
        id,
        entryCount,
        slice(buf, CONFIG_OFFSET, configSize),
        slice(buf, headerSize, entryCount * 4),
        slice(buf, entriesOffset, entryDataSize));
  }

  private ResTableType(
      int headerSize,
      int chunkSize,
      int id,
      int entryCount,
      ByteBuffer config,
      ByteBuffer entryOffsets,
      ByteBuffer entryData) {
    super(CHUNK_RES_TABLE_TYPE, headerSize, chunkSize);
    this.id = id;
    this.entryCount = entryCount;
    this.config = config;
    this.entryOffsets = entryOffsets;
    this.entryData = entryData;
  }

  int getResourceType() {
    return id + 1;
  }
}
