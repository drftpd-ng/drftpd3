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

package org.drftpd.master.slaveselection.filter.maxtransfers;

import org.drftpd.common.util.PropertyHelper;
import org.drftpd.common.vfs.InodeHandleInterface;
import org.drftpd.master.GlobalContext;
import org.drftpd.master.exceptions.FatalException;
import org.drftpd.master.exceptions.NoAvailableSlaveException;
import org.drftpd.master.slavemanagement.RemoteSlave;
import org.drftpd.master.slavemanagement.SlaveManager;
import org.drftpd.master.slavemanagement.SlaveStatus;
import org.drftpd.master.slaveselection.filter.Filter;
import org.drftpd.master.slaveselection.filter.ScoreChart;
import org.drftpd.master.usermanager.User;
import org.drftpd.slave.exceptions.ObjectNotFoundException;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author surface
 * @version $Id$
 */

/**
 * Example slaveselection entry:
 * <pre>
 * <n>.filter==maxtransfersPerslave
 * <n>.assign=<slavename1>+10 <slavename2>+5
 * </pre>
 * Explanation : it will allow up to 10 transfers on slavename1 and 5 on slavename2
 */

public class MaxtransfersPerslaveFilter extends Filter {

    private final ArrayList<AssignSlave> _assigns;

    private final Pattern _p;
    private final boolean _negateExpr;


    public MaxtransfersPerslaveFilter(int i, Properties p) {
        super(i, p);
        try {
            _assigns = parseAssign(PropertyHelper.getProperty(p, i + ".assign"), GlobalContext.getGlobalContext().getSlaveManager());
            _p = Pattern.compile(PropertyHelper.getProperty(p, i + ".match"), Pattern.CASE_INSENSITIVE);
            _negateExpr = PropertyHelper.getProperty(p, i + ".negate.expression", "false").equalsIgnoreCase("true");
        } catch (Exception e) {
            throw new FatalException(e);
        }
    }

    static ArrayList<AssignSlave> parseAssign(String assign, SlaveManager sm) throws ObjectNotFoundException {
        StringTokenizer st = new StringTokenizer(assign, ", ");
        ArrayList<AssignSlave> assigns = new ArrayList<>();

        while (st.hasMoreTokens()) {
            assigns.add(new AssignSlave(st.nextToken(), sm));
        }
        return assigns;
    }

    @Override
    public void process(ScoreChart scorechart, User user, InetAddress peer,
                        char direction, InodeHandleInterface dir, RemoteSlave sourceSlave)
            throws NoAvailableSlaveException {

        Matcher m = _p.matcher(dir.getPath());
        boolean validPath = _negateExpr != m.find();

        for (Iterator<ScoreChart.SlaveScore> iterator = scorechart.getSlaveScores().iterator(); iterator.hasNext(); ) {
            ScoreChart.SlaveScore slavescore = iterator.next();

            for (AssignSlave assign : _assigns) {
                SlaveStatus status;
                try {
                    status = slavescore.getRSlave().getSlaveStatusAvailable();
                } catch (Exception e) {
                    iterator.remove();
                    continue;
                }
                if ((assign._rslave.getName().equalsIgnoreCase(slavescore.getRSlave().getName()) && status.getTransfers() > assign._maxtransfer) && validPath) {
                    iterator.remove();
                }
            }
        }
    }

    static class AssignSlave {
        private final RemoteSlave _rslave;
        private final int _maxtransfer;

        public AssignSlave(String s, SlaveManager slaveManager)
                throws ObjectNotFoundException {

            int pos = s.indexOf("+");

            if (pos != -1) {
            } else {
                pos = s.indexOf("-");

                if (pos == -1) {
                    throw new IllegalArgumentException(s + " is not a valid assign slave expression");
                }
            }

            String slavename = s.substring(0, pos);

            _rslave = slaveManager.getRemoteSlave(slavename);

            _maxtransfer = Integer.parseInt(s.substring(pos + 1));

        }

        public RemoteSlave getRSlave() {
            return _rslave;
        }

        public long getMaxtransfers() {
            return _maxtransfer;
        }
    }
}
