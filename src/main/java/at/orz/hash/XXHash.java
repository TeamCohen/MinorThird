/*
 * Copyright (C) 2012 tamtam180
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.orz.hash;

import at.orz.hash.EncodeUtils.EndianReader;

/**
 * @author tamtam180 - kirscheless at gmail.com
 *
 */
public class XXHash {
	
	private static final int PRIME1 = (int) 2654435761L;
	private static final int PRIME2 = (int) 2246822519L;
	private static final int PRIME3 = (int) 3266489917L;
	private static final int PRIME4 = 668265263;
	private static final int PRIME5 = 0x165667b1;
	
	public static int digestSmall(byte[] data, int seed, boolean bigendian) {
		
		final EndianReader er = bigendian ? EncodeUtils.BEReader : EncodeUtils.LEReader;
		final int len = data.length;
		final int bEnd = len;
		final int limit = bEnd - 4;
		
		int idx = seed + PRIME1;
		int crc = PRIME5;
		int i = 0;
		
		while (i < limit) {
			crc += er.toInt(data, i) + (idx++);
			crc += Integer.rotateLeft(crc, 17) * PRIME4;
			crc *= PRIME1;
			i += 4;
		}
		
		while (i < bEnd) {
			crc += (data[i] & 0xFF) + (idx++);
			crc *= PRIME1;
			i++;
		}
		
		crc += len;
		
		crc ^= crc >>> 15;
		crc *= PRIME2;
		crc ^= crc >>> 13;
		crc *= PRIME3;
		crc ^= crc >>> 16;
		
		return crc;
		
	}
	
	public static int digestFast32(byte[] data, int seed, boolean bigendian) {
		
		final int len = data.length;
		
		if (len < 16) {
			return digestSmall(data, seed, bigendian);
		}
		
		final EndianReader er = bigendian ? EncodeUtils.BEReader : EncodeUtils.LEReader;
		final int bEnd = len;
		final int limit = bEnd - 16;
		int v1 = seed + PRIME1;
		int v2 = v1 * PRIME2 + len;
		int v3 = v2 * PRIME3;
		int v4 = v3 * PRIME4;
		
		int i = 0;
		int crc = 0;
		while (i < limit) {
			v1 = Integer.rotateLeft(v1, 13) + er.toInt(data, i);
			i += 4;
			v2 = Integer.rotateLeft(v2, 11) + er.toInt(data, i);
			i += 4;
			v3 = Integer.rotateLeft(v3, 17) + er.toInt(data, i);
			i += 4;
			v4 = Integer.rotateLeft(v4, 19) + er.toInt(data, i);
			i += 4;
		}
		
		i = bEnd - 16;
		v1 += Integer.rotateLeft(v1, 17);
		v2 += Integer.rotateLeft(v2, 19);
		v3 += Integer.rotateLeft(v3, 13);
		v4 += Integer.rotateLeft(v4, 11);
		
		v1 *= PRIME1;
		v2 *= PRIME1;
		v3 *= PRIME1;
		v4 *= PRIME1;
		
		v1 += er.toInt(data, i);
		i += 4;
		v2 += er.toInt(data, i);
		i += 4;
		v3 += er.toInt(data, i);
		i += 4;
		v4 += er.toInt(data, i);
		
		v1 *= PRIME2;
		v2 *= PRIME2;
		v3 *= PRIME2;
		v4 *= PRIME2;
		
		v1 += Integer.rotateLeft(v1, 11);
		v2 += Integer.rotateLeft(v2, 17);
		v3 += Integer.rotateLeft(v3, 19);
		v4 += Integer.rotateLeft(v4, 13);
		
		v1 *= PRIME3;
		v2 *= PRIME3;
		v3 *= PRIME3;
		v4 *= PRIME3;
		
		crc = v1 + Integer.rotateLeft(v2, 3) + Integer.rotateLeft(v3, 6) + Integer.rotateLeft(v4, 9);
		crc ^= crc >>> 11;
		crc += (PRIME4 + len) * PRIME1;
		crc ^= crc >>> 15;
		crc *= PRIME2;
		crc ^= crc >>> 13;
		
		return crc;
	}
	
	public static int digestStrong32(byte[] data, int seed, boolean bigendian) {
		
		final int len = data.length;
		
		if (len < 16) {
			return digestSmall(data, seed, bigendian);
		}
		
		final EndianReader er = bigendian ? EncodeUtils.BEReader : EncodeUtils.LEReader;
		final int bEnd = len;
		final int limit = bEnd - 16;
		int v1 = seed + PRIME1;
		int v2 = v1 * PRIME2 + len;
		int v3 = v2 * PRIME3;
		int v4 = v3 * PRIME4;
		
		int i = 0;
		int crc = 0;
		
		while (i < limit) {
			v1 += Integer.rotateLeft(v1, 13);
			v1 *= PRIME1;
			v1 += er.toInt(data, i);
			i += 4;

			v2 += Integer.rotateLeft(v2, 11);
			v2 *= PRIME1;
			v2 += er.toInt(data, i);
			i += 4;

			v3 += Integer.rotateLeft(v3, 17);
			v3 *= PRIME1;
			v3 += er.toInt(data, i);
			i += 4;

			v4 += Integer.rotateLeft(v4, 19);
			v4 *= PRIME1;
			v4 += er.toInt(data, i);
			i += 4;

		}
		
		i = bEnd - 16;
		v1 += Integer.rotateLeft(v1, 17);
		v2 += Integer.rotateLeft(v2, 19);
		v3 += Integer.rotateLeft(v3, 13);
		v4 += Integer.rotateLeft(v4, 11);
		
		v1 *= PRIME1;
		v2 *= PRIME1;
		v3 *= PRIME1;
		v4 *= PRIME1;
		
		v1 += er.toInt(data, i);
		i += 4;
		v2 += er.toInt(data, i);
		i += 4;
		v3 += er.toInt(data, i);
		i += 4;
		v4 += er.toInt(data, i);
		
		v1 *= PRIME2;
		v2 *= PRIME2;
		v3 *= PRIME2;
		v4 *= PRIME2;
		
		v1 += Integer.rotateLeft(v1, 11);
		v2 += Integer.rotateLeft(v2, 17);
		v3 += Integer.rotateLeft(v3, 19);
		v4 += Integer.rotateLeft(v4, 13);
		
		v1 *= PRIME3;
		v2 *= PRIME3;
		v3 *= PRIME3;
		v4 *= PRIME3;
		
		crc = v1 + Integer.rotateLeft(v2, 3) + Integer.rotateLeft(v3, 6) + Integer.rotateLeft(v4, 9);
		crc ^= crc >>> 11;
		crc += (PRIME4 + len) * PRIME1;
		crc ^= crc >>> 15;
		crc *= PRIME2;
		crc ^= crc >>> 13;
		
		return crc;
	}
	
	public static void main(String[] args) {
		System.out.println(PRIME1);
	}
	
}
