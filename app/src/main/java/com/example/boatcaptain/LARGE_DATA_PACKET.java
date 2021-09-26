package com.example.boatcaptain;

import java.util.DuplicateFormatFlagsException;
import android.util.Log;

public class LARGE_DATA_PACKET {
    public static int MAX_CHUNK_SIZE = 117;//maximum allowable size of the data portion in each incoming packet from the boat
    public byte [] m_dataBytes;//the data bytes in the chunk
    public int m_nDataSize;//the # of data bytes in the chunk
    public int m_nChunkIndex;//the index of the chunk (first chunk starts at zero)
    public int m_nTestChecksum;//simple 16-bit sum of all of the data bytes

    LARGE_DATA_PACKET(byte []dataBytes, int nDataSize, int nChunkIndex, int nTestChecksum) {
        if (nDataSize>0) {
            m_dataBytes = new byte[nDataSize];
        }
        else {
            m_dataBytes = null;
        }
        m_nChunkIndex = nChunkIndex;
        m_nDataSize = nDataSize;
        for (int i=0;i<nDataSize;i++) {
            m_dataBytes[i] = dataBytes[i];
        }
        m_nTestChecksum = nTestChecksum;
    }

    /**
     * LookForValidLargeDataPacket: looks at the contents of a data buffer to determine whether or not it contains a valid
     * LARGE_DATA_PACKET sequence of bytes. If it does, store these bytes in the form of a LARGE_DATA_PACKET object and return that object.
     *
     * @param dataBuf the data buffer to check to see whether or not it contains a sequence of bytes that contitutes a valid large data packet.
     * @param nBufIndex the index of the data buffer from where to start looking for a valid "large data packet" sequence of bytes.
     * @return a valid LARGE_DATA_PACKET object if a sequence of bytes can be found that contitues a large data packet. Otherwise, return null.
     */
    public static LARGE_DATA_PACKET LookForValidLargeDataPacket(byte []dataBuf, int nBufIndex) {
        //from code on AMOS, describing contents of data chunk:
        //chunkBuf[0] = 'A'
        //chunkBuf[1] = 'M'
        //chunkBuf[2] = 'O'
        //chunkBuf[3] = 'S'
        //chunkBuf[4] = <chunk index, most sig byte>
        //chunkBuf[5] = <chunk index, middle sig byte>
        //chunkBuf[6] = <chunk index, least sig byte>
        //chunkBuf[7] = <chunk data portion size (CRC not included), most sig byte>
        //chunkBuf[8] = <chunk data portion size (CRC not included), least sig byte>
        //chunkBuf[9] = <first data byte>
        //chunkBuf[10] = <2nd data byte>
        //...
        //chunkBuf[9+#data bytes] = <CRC, most sig byte>
        //chunkBuf[10+#data bytes] = <CRC, least sig byte>
       int nMaxBytes = dataBuf.length;
       for (int i=nBufIndex;i<(nMaxBytes-11);i++) {
           if (dataBuf[i]=='A'&&dataBuf[i+1]=='M'&&dataBuf[i+2]=='O'&&dataBuf[i+3]=='S') {
               //test
               String sTest = String.format("found AMOS text at i = %d\n",i);
               Log.d("debug",sTest);
               //end test
               byte []tempBuf = new byte[4];
               //chunk index
               tempBuf[0] = dataBuf[i+6];
               tempBuf[1] = dataBuf[i+5];
               tempBuf[2] = dataBuf[i+4];
               tempBuf[3] = 0;
               int nChunkIndex = Util.toInt(tempBuf);
               //data portion size
               tempBuf[0] = dataBuf[i+8];
               tempBuf[1] = dataBuf[i+7];
               tempBuf[2] = 0;
               tempBuf[3] = 0;
               int nDataSize = Util.toInt(tempBuf);

               if (nDataSize<0||nDataSize>MAX_CHUNK_SIZE) {
                   Log.d("debug","invalid data size\n");
                   return new LARGE_DATA_PACKET(null,0,0,0);//invalid data size
               }
               else if ((11+i+nDataSize)>nMaxBytes) {//not enough bytes yet
                   //test
                   String sMsg = String.format("only have %d bytes, need %d\n",nMaxBytes,11+i+nDataSize);
                   Log.d("debug",sMsg);
                   //end test
                   return null;
               }
               byte []dataBytes = new byte[nDataSize];
               int checksum_total = 0;
               for (int j=0;j<nDataSize;j++) {
                   dataBytes[j] = dataBuf[9+i+j];
                   checksum_total = checksum_total + (int)(dataBytes[j]&0xff);
               }
               //test checksum
               tempBuf[0] = dataBuf[10 + i + nDataSize];
               tempBuf[1] = dataBuf[9 + i + nDataSize];
               tempBuf[2] = 0;
               tempBuf[3] = 0;
               int nTestChecksum = Util.toInt(tempBuf);
               if (nTestChecksum!=checksum_total) {
                   String sMsg = String.format("invalid checksum, calculated = %d, got = %d\n",checksum_total,nTestChecksum);
                   Log.d("debug",sMsg);
                   return new LARGE_DATA_PACKET(null,0,0,0);//invalid checksum
               }
               LARGE_DATA_PACKET largeDataPacket = new LARGE_DATA_PACKET(dataBytes, nDataSize, nChunkIndex, nTestChecksum);
               return largeDataPacket;//valid large data packet
           }
       }
       //test
        String sTest = String.format("nBytesAvail = %d\n",nMaxBytes-nBufIndex);
       Log.d("debug",sTest);
        // end test
       return null;//not enough data yet to form large data packet
    }

    /**
     * isValid: check to see if this large data packet object contains valid data or not
     * @return true if this object contains valid data, false otherwise.
     */
    public boolean isValid() {//return true if this large data packet object is valid or not
        return (m_dataBytes!=null&&m_nDataSize>0);
    }

    /**
     * appendBytesTo: append the data bytes from this large data packet to the buffer, buf starting at nBufIndex.
     * @param buf the buffer to which to append the data bytes from this large data packet.
     * @param nBufIndex the index within buf at which to append the data bytes from this large data packet. Any bytes previously present in buf at or beyond nBufIndex are not included in the returned result.
     * @return a new buffer which contains the contents of buf up to nBufIndex, followed by the data bytes from this large data packet.
     */
    byte [] appendBytesTo(byte []buf, int nBufIndex) {
        byte []retVal = new byte[nBufIndex + m_nDataSize];
        for (int i=0;i<nBufIndex;i++) {
            retVal[i] = buf[i];
        }
        for (int i=0;i<this.m_nDataSize;i++) {
            retVal[i+nBufIndex] = m_dataBytes[i];
        }
        return retVal;
    }


    /**
     * GetChunkIndex: get the index associated with this large chunk of data
     * @return the zero-based index of this large data chunk
     */
    public int GetChunkIndex() {
        return this.m_nChunkIndex;
    }


    /**
     * GetCRCBytes: get the two CRC bytes associated with this large data chunk
     * @return two byte array corresponding to the 2 CRC bytes associated with this large data chunk
     */
    public byte [] GetCRCBytes() {
        byte [] tempBytes = Util.toByteArray(this.m_nTestChecksum);
        byte [] retVal = new byte[2];
        retVal[0] = tempBytes[2];
        retVal[1] = tempBytes[3];
        return retVal;
    }

    /**
     * RemoveLastPartialChunk: remove all the data from buf after and including nBufIndex
     * @param buf the buffer from which to remove the last part, starting at nBufIndex.
     * @param nBufIndex the index of the buffer buf, at which we will discard the remaining data.
     * @return a new buffer that includes just the first part of buf, up to the index nBufIndex.
     */
    public byte [] RemoveLastPartialChunk(byte []buf, int nBufIndex) {
        byte []retVal = new byte[nBufIndex];
        for (int i=0;i<nBufIndex;i++) {
            retVal[i] = buf[i];
        }
        return retVal;
    }
}
