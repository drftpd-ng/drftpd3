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
package org.drftpd.zipscript.common.sfv;

import org.drftpd.common.network.AsyncResponse;

/**
 * @author zubov
 * @version $Id$
 */
@SuppressWarnings("serial")
public class AsyncResponseSFVInfo extends AsyncResponse {
    private final SFVInfo _sfv;

    public AsyncResponseSFVInfo(String index, SFVInfo sfv) {
        super(index);

        if (sfv == null) {
            throw new IllegalArgumentException("sfv cannot be null");
        }

        _sfv = sfv;
    }

    public SFVInfo getSFV() {
        return _sfv;
    }

    public String toString() {
        return getClass().getName();
    }
}
