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
package org.drftpd.find.master;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.drftpd.common.util.Bytes;
import org.drftpd.master.GlobalContext;
import org.drftpd.master.commands.*;
import org.drftpd.master.indexation.AdvancedSearchParams;
import org.drftpd.master.indexation.IndexEngineInterface;
import org.drftpd.master.indexation.IndexException;
import org.drftpd.master.network.Session;
import org.drftpd.master.usermanager.User;
import org.drftpd.master.vfs.DirectoryHandle;
import org.drftpd.master.vfs.FileHandle;
import org.drftpd.master.vfs.InodeHandle;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author scitz0
 * @version $Id$
 */
public class Search extends CommandInterface {

    public static final Logger logger = LogManager.getLogger(Find.class);

    private ResourceBundle _bundle;

    public void initialize(String method, String pluginName, StandardCommandManager cManager) {
        super.initialize(method, pluginName, cManager);
        _bundle = cManager.getResourceBundle();
    }

    public CommandResponse doSEARCH(CommandRequest request) throws ImproperUsageException {
        if (!request.hasArgument()) {
            throw new ImproperUsageException();
        }

        AdvancedSearchParams params = new AdvancedSearchParams();

        if (request.getProperties().getProperty("exact", "false").equals("true")) {
            params.setExact(request.getArgument());
        } else {
            params.setName(request.getArgument());
        }

        String type = request.getProperties().getProperty("type");
        if (type != null && type.equals("d")) {
            params.setInodeType(AdvancedSearchParams.InodeType.DIRECTORY);
        } else if (type != null && type.equals("f")) {
            params.setInodeType(AdvancedSearchParams.InodeType.FILE);
        } else {
            logger.error("Incorrect type specified for search function");
            return new CommandResponse(500, "Internal Server error occurred, stopping execution");
        }

        int limit = Integer.parseInt(request.getProperties().getProperty("limit", "5"));

        // Get all results, we filter out hidden inodes later
        params.setLimit(0);

        IndexEngineInterface ie = GlobalContext.getGlobalContext().getIndexEngine();
        Map<String, String> inodes;

        try {
            inodes = ie.advancedFind(GlobalContext.getGlobalContext().getRoot(), params, "doSEARCH");
        } catch (IndexException e) {
            logger.error(e.getMessage());
            return new CommandResponse(550, e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.info(e.getMessage());
            return new CommandResponse(550, e.getMessage());
        }

        Map<String, Object> env = new HashMap<>();

        User user = request.getSession().getUserNull(request.getUser());

        Session session = request.getSession();

        CommandResponse response = new CommandResponse(200, "Search complete");

        LinkedList<String> responses = new LinkedList<>();

        boolean observePrivPath = request.getProperties().getProperty("observe.privpath", "true").
                equalsIgnoreCase("true");

        InodeHandle inode;
        for (Map.Entry<String, String> item : inodes.entrySet()) {
            if (responses.size() == limit) {
                break;
            }
            try {
                inode = item.getValue().equals("d") ? new DirectoryHandle(item.getKey().substring(0, item.getKey().length() - 1)) :
                        new FileHandle(item.getKey());
                if (observePrivPath ? inode.isHidden(user) : inode.isHidden(null)) {
                    continue;
                }
                env.put("name", inode.getName());
                env.put("path", inode.getPath());
                env.put("owner", inode.getUsername());
                env.put("group", inode.getGroup());
                env.put("size", Bytes.formatBytes(inode.getSize()));
                responses.add(session.jprintf(_bundle, "search.item", env, user.getName()));
            } catch (FileNotFoundException e) {
                logger.warn("Index contained an non-existent inode: {}", item.getKey());
            }
        }

        if (responses.isEmpty()) {
            response.addComment(session.jprintf(_bundle, "search.empty", env, user.getName()));
            return response;
        }

        env.put("limit", limit);
        env.put("results", responses.size());
        response.addComment(session.jprintf(_bundle, "search.header", env, user.getName()));

        for (String line : responses) {
            response.addComment(line);
        }

        return response;
    }

}
