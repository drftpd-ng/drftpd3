/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrFTPD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DrFTPD; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.drftpd.usermanager;

import org.apache.log4j.Logger;

import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

import java.net.InetAddress;


/**
 * @author mog
 * @version $Id: HostMask.java 808 2004-11-18 15:58:39Z mog $
 */
public class HostMask {
    private static final Logger logger = Logger.getLogger(HostMask.class);
    private String _hostMask;
    private String _identMask;

    public HostMask(String string) {
        int pos = string.indexOf('@');

        if (pos == -1) {
            _identMask = null;
            _hostMask = string;
        } else {
            _identMask = string.substring(0, pos);
            _hostMask = string.substring(pos + 1);
        }
    }

    public String getHostMask() {
        return _hostMask;
    }

    public String getIdentMask() {
        return _identMask;
    }

    public String getMask() {
        return getIdentMask() + "@" + getHostMask();
    }

    /**
     * Is ident used?
     * @return false is ident mask equals "*"
     */
    public boolean isIdentMaskSignificant() {
        return (_identMask != null) && !_identMask.equals("*");
    }

    public boolean matchesHost(InetAddress a) throws MalformedPatternException {
        Perl5Matcher m = new Perl5Matcher();
        GlobCompiler c = new GlobCompiler();
        Pattern p = c.compile(getHostMask());

        return (m.matches(a.getHostAddress(), p) ||
        m.matches(a.getHostName(), p));
    }

    public boolean matchesIdent(String ident) throws MalformedPatternException {
        Perl5Matcher m = new Perl5Matcher();
        GlobCompiler c = new GlobCompiler();

        if (ident == null) {
            ident = "";
        }

        return !isIdentMaskSignificant() ||
        m.matches(ident, c.compile(getIdentMask()));
    }

    public String toString() {
        if (_identMask != null) {
            return _identMask + "@" + _hostMask;
        }

        return "*@" + _hostMask;
    }
}