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
package org.drftpd.imdb.master.find;

import org.drftpd.common.dynamicdata.KeyNotFoundException;
import org.drftpd.find.master.FindSettings;
import org.drftpd.find.master.FindUtils;
import org.drftpd.find.master.option.OptionInterface;
import org.drftpd.imdb.master.index.IMDBQueryParams;
import org.drftpd.master.commands.ImproperUsageException;
import org.drftpd.master.indexation.AdvancedSearchParams;

import java.util.Map;

/**
 * @author scitz0
 * @version $Id: MP3Option.java 2491 2011-07-11 21:56:53Z scitz0 $
 */
public class IMDBOption implements OptionInterface {

    private final Map<String, String> _options = Map.of(
            "imdbtitle", "<name> # Search for imdb releases matching provided name",
            "imdbdirector", "<name> # Search for imdb releases that are directed by provided name",
            "imdbgenres", "<name> # Search for imdb releases that have provided genre name",
            "imdbvotes", "<min votes>:<max votes> # Search for imdb releases that fall between min votes and max votes",
            "imdbrating", "<min rating>:<max rating> # Search for imdb releases that fall between min rating and max rating",
            "imdbyear", "<min year>:<max year> # Search for imdb releases that fall between min year and max year",
            "imdbruntime", "<min runtime>:<max runtime> # Search for imdb releases that fall between min runtime and max runtime"
    );

    @Override
    public Map<String, String> getOptions() {
        return _options;
    }

    @Override
    public void executeOption(String option, String[] args, AdvancedSearchParams params, FindSettings settings) throws ImproperUsageException {
        if (args == null) {
            throw new ImproperUsageException("Missing argument for " + option + " option");
        }
        IMDBQueryParams queryParams;
        try {
            queryParams = params.getExtensionData(IMDBQueryParams.IMDBQUERYPARAMS);
        } catch (KeyNotFoundException e) {
            queryParams = new IMDBQueryParams();
            params.addExtensionData(IMDBQueryParams.IMDBQUERYPARAMS, queryParams);
        }
        if (option.equalsIgnoreCase("-imdbtitle")) {
            queryParams.setTitle(args[0]);
        } else if (option.equalsIgnoreCase("-imdbdirector")) {
            queryParams.setDirector(args[0]);
        } else if (option.equalsIgnoreCase("-imdbgenres")) {
            queryParams.setGenres(args[0]);
        } else if (option.equalsIgnoreCase("-imdbvotes")) {
            Integer[] range = getIntRange(args[0]);
            queryParams.setMinVotes(range[0]);
            queryParams.setMaxVotes(range[1]);
        } else if (option.equalsIgnoreCase("-imdbrating")) {
            Integer[] range = getIntRange(args[0]);
            queryParams.setMinRating(range[0]);
            queryParams.setMaxRating(range[1]);
        } else if (option.equalsIgnoreCase("-imdbyear")) {
            Integer[] range = getIntRange(args[0]);
            queryParams.setMinYear(range[0]);
            queryParams.setMaxYear(range[1]);
        } else if (option.equalsIgnoreCase("-imdbruntime")) {
            Integer[] range = getIntRange(args[0]);
            queryParams.setMinRuntime(range[0]);
            queryParams.setMaxRuntime(range[1]);
        }
        params.setInodeType(AdvancedSearchParams.InodeType.DIRECTORY);
    }

    private Integer[] getIntRange(String arg) throws ImproperUsageException {
        Integer[] intRange = new Integer[2];
        try {
            String[] range = FindUtils.getRange(arg, ":");
            if (range[0] != null && range[1] != null) {
                int from = Integer.parseInt(range[0].replaceAll("[,.]", ""));
                int to = Integer.parseInt(range[1].replaceAll("[,.]", ""));
                if (from > to) {
                    throw new ImproperUsageException("Range invalid, min value higher than max");
                }
                intRange[0] = from;
                intRange[1] = to;
            } else if (range[0] != null) {
                intRange[0] = Integer.valueOf(range[0].replaceAll("[,.]", ""));
            } else if (range[1] != null) {
                intRange[0] = 0; // We dont want to search for negative values
                intRange[1] = Integer.valueOf(range[1].replaceAll("[,.]", ""));
            }
        } catch (NumberFormatException e) {
            throw new ImproperUsageException(e);
        }
        return intRange;
    }

}
