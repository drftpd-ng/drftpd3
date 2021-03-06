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
package org.drftpd.request.master.event;

import org.drftpd.master.event.Event;
import org.drftpd.master.usermanager.User;
import org.drftpd.master.vfs.DirectoryHandle;

/**
 * Event that represents a REQUEST/REQFILLED/REQDELETE event.
 *
 * @author fr0w
 * @version $Id$
 */
public class RequestEvent extends Event {

    private final User _issuer;
    private final User _requestOwner;
    private final DirectoryHandle _requestRoot;
    private final String _requestName;

    /**
     * This constructor is useful for REQUEST events since the {@code request owner} is equals to {@code command issuer}
     *
     * @param command      the type of the command that generated this event {@code request/reqfilled/reqdelete}
     * @param requestRoot  the request root (ex: /requests/)
     * @param requestOwner the user who requested
     * @param requestName  what the user requested
     */
    public RequestEvent(String command, DirectoryHandle requestRoot, User requestOwner, String requestName) {
        this(command, requestOwner, requestRoot, requestOwner, requestName);
    }

    /**
     * This constructor is useful for REQFILLED/REQDELETE events since the {@code request owner} is <b>not</b> equals to {@code command issuer}
     *
     * @param command       the type of the command that generated this event {@code request/reqfilled/reqdelete}
     * @param requestRoot   the request root (ex: /requests/)
     * @param requestOwner  the user who requested
     * @param commandIssuer the user who issued the command that fired this event.
     * @param requestName   what the user requested
     */
    public RequestEvent(String command, User commandIssuer, DirectoryHandle requestRoot, User requestOwner, String requestName) {
        super(command);

        _issuer = commandIssuer;
        _requestRoot = requestRoot;
        _requestOwner = requestOwner;
        _requestName = requestName;
    }

    public User getCommandIssuer() {
        return _issuer;
    }

    public User getRequestOwner() {
        return _requestOwner;
    }

    public DirectoryHandle getRequestRoot() {
        return _requestRoot;
    }

    public String getRequestName() {
        return _requestName;
    }

}