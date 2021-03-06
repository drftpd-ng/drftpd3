/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * DrFTPD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DrFTPD; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.drftpd.zipscript.slave.mp3.decoder;

/**
 * Exception error codes for components of the JavaLayer API.
 *
 * @author Originally taken from JLayer - http://www.javazoom.net/javalayer/javalayer.html
 * @version $Id$
 */
public interface JavaLayerErrors {
    /**
     * The first bitstream error code. See the {@link DecoderErrors DecoderErrors}
     * interface for other bitstream error codes.
     */
    int BITSTREAM_ERROR = 0x100;

    /**
     * The first decoder error code. See the {@link DecoderErrors DecoderErrors}
     * interface for other decoder error codes.
     */
    int DECODER_ERROR = 0x200;
}