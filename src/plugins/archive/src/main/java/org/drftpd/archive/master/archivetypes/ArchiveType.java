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
package org.drftpd.archive.master.archivetypes;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.drftpd.archive.master.Archive;
import org.drftpd.archive.master.DuplicateArchiveException;
import org.drftpd.common.extensibility.PluginInterface;
import org.drftpd.common.util.PropertyHelper;
import org.drftpd.jobs.master.Job;
import org.drftpd.jobs.master.JobManager;
import org.drftpd.master.GlobalContext;
import org.drftpd.master.sections.SectionInterface;
import org.drftpd.master.slavemanagement.RemoteSlave;
import org.drftpd.master.vfs.DirectoryHandle;
import org.drftpd.master.vfs.FileHandle;
import org.drftpd.master.vfs.ObjectNotValidException;
import org.drftpd.master.vfs.VirtualFileSystem;
import org.drftpd.slave.exceptions.FileExistsException;
import org.drftpd.slave.exceptions.ObjectNotFoundException;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author CyBeR
 * @version $Id$
 */
public abstract class ArchiveType {

    private static final Logger logger = LogManager.getLogger(ArchiveType.class);

    protected Archive _parent;

    // Current section directory is in.
    protected SectionInterface _section;

    // Current .conf loop number we are on.  Used for other archive types to grab extra configurations
    // Any positive number is valid. 0 means manual (ie: create by site archive) and any number above 1 means it's in archive.conf
    protected int _confNum;

    protected Set<RemoteSlave> _slaveList;

    // Used For: number of slaves to archive too
    protected int _numOfSlaves;

    // Uses For: setting priority vs other archives/jobs
    protected int _priority;

    // Used for: moving directory to another folder
    protected DirectoryHandle _archiveToFolder;

    // Used for: setting destination directory ONLY after moving it (more for events)
    protected DirectoryHandle _destinationDirectory;

    // Used for: a specific type of folder to archive too.  (Alpha/Dated)
    protected String _archiveDirType;

    // Check to see if we are going to move the release after archive
    protected boolean _moveRelease;

    // Checks to see if we are moving the release only (no slave -> slave archive)
    protected boolean _moveReleaseOnly;

    // Used for: Archive dirs After This amount of time
    private long _archiveAfter;

    // Do not archive dirs older than this
    private long _ignoreAfter;

    // Current directory being archived
    private DirectoryHandle _directory;

    // Used for: archiving only dirs that match this regex form. (Default is .*)
    private String _archiveRegex;

    // Used for: how many times to repeat during each cycle
    private int _repeat;

    // Used for holding all failed directories during each cycle
    private ArrayList<String> _failedDirs;

    // Used for scanning subdirs of archived dir, instead of just the parent dir
    private boolean _scansubdirs;

    /**
     * Sets _slaveList, _numOfSlaves, _section, _parent, and _archiveAfter Each
     * implementing ArchiveType still needs to check/validate these figures to
     * their specific needs After the constructor finishes, _slaveList is
     * guaranteed to be non-null, but can still be empty
     *
     * @param archive Our Archive Manager instance
     * @param section The section we are working on
     * @param p The configuration for this Archive Type
     * @param confNum The number this Archive Type has in the configuration
     */
    public ArchiveType(Archive archive, SectionInterface section, Properties p, int confNum) {
        if (confNum < 0) {
            logger.error("Incorrect Configuration Number found [{}], needs to be positive", confNum);
        }
        // Initialize this to 0
        _numOfSlaves = 0;
        _confNum = confNum;
        _parent = archive;
        _section = section;
        setProperties(p);
    }

    /*
     * Function to get the GlobalContext, this is here so we can override it during unit testing
     */
    public GlobalContext getGlobalContext() {
        return getParent().getGlobalContext();
    }

    public Archive getParent() {
        return _parent;
    }

    public int getConfNum() {
        return _confNum;
    }

    /*
     * Checks to see if files are archived to the all the slaves configured
     */
    protected static boolean isArchivedToAllSlaves(DirectoryHandle lrf, int x) throws OfflineSlaveException {
        HashSet<RemoteSlave> slaveSet = null;
        Set<DirectoryHandle> directories;
        Set<FileHandle> files;
        try {
            directories = lrf.getDirectoriesUnchecked();
            files = lrf.getFilesUnchecked();
        } catch (FileNotFoundException e1) {
            // directory doesn't exist, no files to archive
            return true;
        }

        for (DirectoryHandle directory : directories) {
            if (!isArchivedToAllSlaves(directory, x)) {
                return false;
            }
        }

        for (FileHandle file : files) {
            Collection<RemoteSlave> availableSlaves;
            try {
                if (!file.isAvailable()) {
                    throw new OfflineSlaveException(file.getPath() + " is offline");
                }
                availableSlaves = file.getSlaves();
            } catch (FileNotFoundException e) {
                // can't archive a directory with files that have been moved,
                // we'll come back later
                return true;
            }

            if (slaveSet == null) {
                slaveSet = new HashSet<>(availableSlaves);
            } else {
                if (!(slaveSet.containsAll(availableSlaves) && availableSlaves.containsAll(slaveSet))) {
                    return false;
                }
            }
        }

        if (slaveSet == null) { // no files found in directory
            return true;
        }

        for (RemoteSlave rslave : slaveSet) {
            if (!rslave.isAvailable()) {
                throw new OfflineSlaveException(rslave.getName() + " is offline");
            }
        }

        return (slaveSet.size() == x);
    }

    /*
     * Checks to see if the files are archived to the specific slaves specified
     */
    protected static boolean isArchivedToSpecificSlaves(DirectoryHandle lrf, int x, Set<RemoteSlave> rslaves) throws OfflineSlaveException {
        Set<DirectoryHandle> directories;
        Set<FileHandle> files;
        try {
            directories = lrf.getDirectoriesUnchecked();
            files = lrf.getFilesUnchecked();
        } catch (FileNotFoundException e1) {
            // directory doesn't exist, no files to archive
            return true;
        }

        for (DirectoryHandle directory : directories) {
            if (!isArchivedToSpecificSlaves(directory, x, rslaves)) {
                return false;
            }
        }

        for (FileHandle file : files) {
            Collection<RemoteSlave> availableSlaves;
            try {
                if (!file.isAvailable()) {
                    throw new OfflineSlaveException(file.getPath() + " is offline");
                }
                availableSlaves = file.getSlaves();
            } catch (FileNotFoundException e) {
                // can't archive a directory with files that have been moved,
                // we'll come back later
                return true;
            }


            int checknumslaves = 0;
            for (RemoteSlave fileslaves : availableSlaves) {
                for (RemoteSlave listslaves : rslaves) {
                    if (listslaves.getName().equals(fileslaves.getName())) {
                        checknumslaves++;
                    }
                }
            }
            if (checknumslaves < x) {
                return false;
            }

        }
        return true;
    }

    /**
     * Used to determine a list of slaves specified in from the conf file
     *
     * @return Set<RemoteSlave> Set of destination slaves
     */
    public abstract Set<RemoteSlave> findDestinationSlaves();

    /**
     * if the directory is archived by this type's definition, this method
     * returns true
     *
     * @throws IncompleteDirectoryException If a Directory is not complete yet
     * @throws OfflineSlaveException If a slave is offline
     * @throws FileNotFoundException If a file is not found
     */
    protected abstract boolean isArchivedDir(DirectoryHandle directory) throws IncompleteDirectoryException, OfflineSlaveException, FileNotFoundException;

    /*
     * Returns the string eqivilant for the archive type
     */
    public abstract String toString();

    /*
     * Returns the current directory being archived
     */
    public final DirectoryHandle getDirectory() {
        return _directory;
    }

    /*
     * Sets the current directory to be archived.
     */
    public final void setDirectory(DirectoryHandle lrf) {
        _directory = lrf;
    }

    /*
     * Returns how many times loop should repeat during each cycle
     */
    public final int getRepeat() {
        return _repeat;
    }

    /*
     * This is used for getting all non archived dirs.
     * In case of a Section with "sub sections" this handles that as well.
     */
    private void getOldestNonArchivedDir2(ArrayList<DirectoryHandle> oldDirs, DirectoryHandle lrf) {
        if (checkFailedDir(lrf.getPath())) {
            return;
        }

        try {
            // Checks regex to see if dir should be archived or not
            if (lrf.getName().matches(getArchiveRegex())) {
                //make sure dir is archived before moving it
                long age = System.currentTimeMillis() - lrf.creationTime();
                if (!isArchivedDir(lrf)) {
                    if (age > getArchiveAfter() && (age < getIgnoreAfter() || getIgnoreAfter() == -1)) {
                        oldDirs.add(lrf);
                    }
                } else {
                    //move release to dest folder if needed
                    if (_moveRelease) {
                        if (age > getArchiveAfter() && (age < getIgnoreAfter() || getIgnoreAfter() == -1)) {
                            oldDirs.add(lrf);
                        }
                    }
                }
            }
        } catch (IncompleteDirectoryException e) {
            logger.debug("Received IncompleteDirectoryException", e);
        } catch (OfflineSlaveException e) {
            logger.debug("Received OfflineSlaveException", e);
        } catch (FileNotFoundException e) {
            logger.debug("Received FileNotFoundException", e);
        }
    }

    /**
     * Returns the oldest LinkedRemoteFile(directory) that needs to be archived
     * by this type's definition If no such directory exists, it returns null
     * <p>
     * Checks dir by regex, and by creationTime.
     */
    public final DirectoryHandle getOldestNonArchivedDir() {
        ArrayList<DirectoryHandle> oldDirs = new ArrayList<>();

        try {
            DirectoryHandle dir = getSection().getCurrentDirectory();

            // checks to see if the section is a dated section
            // done this way so don't have to load other classes to figure it out
            if (!getSection().getCurrentDirectory().equals(getSection().getBaseDirectory())) {
                dir = getSection().getBaseDirectory();
            }

            for (DirectoryHandle lrf : dir.getDirectoriesUnchecked()) {
                try {
                    _parent.checkPathForArchiveStatus(lrf.getPath());
                } catch (DuplicateArchiveException e1) {
                    /*
                     *	we are already archiving something for this path..
                     *  ..lets wait until thats done before we continue
                     */
                    logger.debug("{} - Already archiving something from this path. Skip it.", getClass().toString());
                    continue;
                }

                if (_scansubdirs) {
                    for (DirectoryHandle lrf2 : lrf.getDirectoriesUnchecked()) {
                        // This attempts to skip specific directory names
                        // season.(any number)
                        // any numbers behind each other (1002)
                        // any numbers followed by a non-word character followed by a number (2008-12)
                        // Any numbers followed by a non-word character followed by a number followed by a non-word character followed by a number (2008-12-10)
                        if (lrf2.getName().matches("(?i)^(season.*|(\\d+|\\d+\\W\\d+|\\d+\\W\\d+\\W\\d+)$)")) // this matches season.\d+ and datum formats number, number-number, number-number-number
                        {
                            for (DirectoryHandle lrf3 : lrf2.getDirectoriesUnchecked()) {
                                getOldestNonArchivedDir2(oldDirs, lrf3);
                            }
                        } else {
                            getOldestNonArchivedDir2(oldDirs, lrf2);
                        }
                    }
                } else {
                    // we do this check so we can't move a dated dir
                    if (getSection().getCurrentDirectory().equals(lrf) && (_moveRelease)) {
                        continue;
                    }
                    getOldestNonArchivedDir2(oldDirs, lrf);
                }

            }
        } catch (FileNotFoundException e) {
            logger.debug("We received a FileNotFoundException and assume that either section does not exist and/or no directories to archive");
            // section does not exist, no directories to archive
            // list is empty so the rest of the code will handle that
        }

        DirectoryHandle oldestDir = null;
        long oldestDirCT = 0;
        for (Iterator<DirectoryHandle> iter = oldDirs.iterator(); iter.hasNext(); ) {
            DirectoryHandle temp = iter.next();

            if (oldestDir == null) {
                oldestDir = temp;
                try {
                    oldestDirCT = oldestDir.creationTime();
                } catch (FileNotFoundException e) {
                    oldestDir = null;
                    iter.remove();
                }
                continue;
            }
            try {
                if (oldestDirCT > temp.creationTime()) {
                    oldestDir = temp;
                }
            } catch (FileNotFoundException e) {
                iter.remove();
            }
        }
        if (oldestDir != null) {
            logger.debug("{} - Returning the oldest directory {}", getClass().toString(), oldestDir);
            return oldestDir;
        }
        logger.debug("{} - All directories are archived", getClass().toString());
        return null;
    }

    /*
     * Gets the jobmananger, hopefully its loaded.
     */
    public JobManager getJobManager() {
        for (PluginInterface plugin : getGlobalContext().getPlugins()) {
            if (plugin instanceof JobManager) {
                return (JobManager) plugin;
            }
        }
        throw new RuntimeException("JobManager is not loaded");
    }

    /**
     * Adds relevant Jobs to the JobManager and returns an ArrayList of those
     * Job's
     *
     * @throws FileNotFoundException If the requested directory is not found
     */
    public ArrayList<Job> send() throws FileNotFoundException {
        ArrayList<Job> jobs = recursiveSend(getDirectory());
        JobManager jm = getJobManager();
        jm.addJobsToQueue(jobs);
        return jobs;
    }

    /*
     * Adds the files in the directory (and subdirs) to the job manager to be archived
     */
    protected ArrayList<Job> recursiveSend(DirectoryHandle lrf) throws FileNotFoundException {
        ArrayList<Job> jobQueue = new ArrayList<>();

        for (DirectoryHandle directoryHandle : lrf.getDirectoriesUnchecked()) {
            jobQueue.addAll(recursiveSend(directoryHandle));
        }
        for (FileHandle file : lrf.getFilesUnchecked()) {
            logger.info("Adding {} to the job queue", file.getPath());
            Job job = new Job(file, _priority, _numOfSlaves, findDestinationSlaves());
            jobQueue.add(job);
        }

        return jobQueue;
    }

    /*
     * Checks to see if directory is currently being archived.
     */
    public final boolean isBusy() {
        return (getDirectory() != null);
    }

    /*
     * Returns when to archive files (after X duration)
     */
    protected final long getArchiveAfter() {
        return _archiveAfter;
    }

    /*
     * Returns when not archive files any more
     */
    protected final long getIgnoreAfter() {
        return _ignoreAfter;
    }

    /*
     * Returns section that directory is in
     */
    public final SectionInterface getSection() {
        return _section;
    }

    /*
     * Returns the REGEX string for directory checks
     */
    public final String getArchiveRegex() {
        return _archiveRegex;
    }

    /**
     * Sets standard properties for this ArchiveType
     */
    private void setProperties(Properties properties) {
        _failedDirs = new ArrayList<>();
        _archiveAfter = 60000 * Long.parseLong(PropertyHelper.getProperty(properties, _confNum + ".archiveafter", "0").trim());
        _ignoreAfter = Long.parseLong(PropertyHelper.getProperty(properties, _confNum + ".ignoreafter", "-1").trim());
        _numOfSlaves = Integer.parseInt(properties.getProperty(_confNum + ".numofslaves", "0").trim());
        _repeat = Integer.parseInt(properties.getProperty(_confNum + ".repeat", "1").trim());
        _scansubdirs = properties.getProperty(_confNum + ".scansubdirs", "0").trim().equals("1");

        // Guard that repeat is not negative
        if (_repeat < 1) {
            logger.warn("repeat cannot be negative. Got {}, setting to 1", _repeat);
            _repeat = 1;
        }

        // Set it to milliseconds (input is in minutes)
        if (_ignoreAfter > 0) {
            _ignoreAfter = 60000 * _ignoreAfter;
        }

        /*
         * Grabs archiveRegex property to check if archive dir matches.  If empty, archives all dirs
         */
        _archiveRegex = properties.getProperty(_confNum + ".archiveregex", ".*").trim();
        try {
            Pattern.compile(_archiveRegex);
        } catch (PatternSyntaxException e) {
            logger.error("Regex Entry for {} is invalid", _confNum);
            _archiveRegex = ".*";
        }

        /*
         * Gets toDirectory property to check if the folder should be moved after archiving
         */
        _archiveToFolder = null;
        _destinationDirectory = null;
        _moveRelease = false;
        _archiveDirType = "";
        String _moveToDirProp = properties.getProperty(_confNum + ".todirectory", "").trim();
        if (!_moveToDirProp.equals("")) {
            SectionInterface sec = getGlobalContext().getSectionManager().getSection(_moveToDirProp);
            if (sec.getName().isEmpty()) {
                try {
                    DirectoryHandle moveInode = new DirectoryHandle(_moveToDirProp);
                    if (!moveInode.exists()) {
                        // dir doesn't exist.  Lets check if Parent does
                        if (!moveInode.getParent().exists()) {
                            // parent dir doesn't exist either, really don't wanna make 3 sets of dirs.
                            logger.error("Directory and ParentDirectory for conf number '{}' Not Found: {}", _confNum, _moveToDirProp);
                        } else {
                            // Parent Exists = Good we can do this
                            _archiveToFolder = moveInode;
                        }
                    } else {
                        // Destination exists = Good we can do this
                        _archiveToFolder = moveInode;
                    }
                } catch (IllegalArgumentException e) {
                    logger.debug("Received IllegalArgumentException, we assume a directory {} does not exist", _moveToDirProp);
                    // Directory does not exist.
                }
            } else {
                // Section exists = Good we can do this
                _archiveToFolder = sec.getCurrentDirectory();
            }

            if (_archiveToFolder != null) {
                _moveRelease = true;

                /*
                 * If a dir/section is selected, check to see if a specific type of subdir needs to be created.
                 */
                _archiveDirType = properties.getProperty(_confNum + ".todirectorytype", "").trim();
            }
        }

        HashSet<RemoteSlave> destSlaves = new HashSet<>();

        for (int i = 1; ; i++) {
            String slaveName = PropertyHelper.getProperty(properties, _confNum + ".slavename." + i, "");
            if (slaveName.length() <= 0) {
                // We are done
                break;
            }

            slaveName = slaveName.trim();

            try {
                RemoteSlave remoteSlave = getGlobalContext().getSlaveManager().getRemoteSlave(slaveName);
                destSlaves.add(remoteSlave);
            } catch (ObjectNotFoundException e) {
                logger.error("Unable to get slave {} from the SlaveManager", slaveName);
            }
        }
        _slaveList = destSlaves;

        /*
         * Checks to see if any slaves were in the conf file and
         * if none were to check it the conf wants to move the rls which sets the
         * variable correctly.
         */
        _moveReleaseOnly = ((_slaveList.isEmpty()) && (_moveRelease));

        _priority = Integer.parseInt(properties.getProperty(_confNum + ".priority", "3").trim());
    }

    /*
     * If this ArchiveType was created using manual 'site archive' this will return true otherwise false
     */
    public boolean isManual() {
        return _confNum == 0;
    }

    /*
     * Sets the slaves to archive too (used for archive command only)
     */
    public final void setRSlaves(Set<RemoteSlave> slaveList) {
        _slaveList = slaveList;
    }

    /*
     * Loops through and waits for all files to archive to configured slaves.
     */
    public final void waitForSendOfFiles(ArrayList<Job> jobQueue) {
        do {
            for (Iterator<Job> iter = jobQueue.iterator(); iter.hasNext(); ) {
                Job job = iter.next();

                if (job.isDone()) {
                    logger.debug("{} - is done being sent", job);
                    iter.remove();
                }
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }

        } while (!jobQueue.isEmpty());
    }

    /*
     * Loops though all the slaves, and returns all current slaves for archive
     */
    protected String outputSlaves(Collection<RemoteSlave> slaveList) {

        String answer = "Empty";

        if (slaveList.size() > 0) {
            Set<String> slaves = new HashSet<>();
            for (RemoteSlave remoteSlave : slaveList) {
                slaves.add(remoteSlave.getName());
            }
            answer = StringUtils.join(slaves, ',');
        }

        return answer;
    }

    /*
     * Recursively Create parent directories
     */
    protected boolean createDirectories(DirectoryHandle dir) {
        if (dir.isRoot()) {
            // We are at the root and done
            return true;
        }

        if (!dir.exists()) {
            if (!dir.getParent().exists()) {
                if (!createDirectories(dir.getParent())) {
                    return false;
                }
            }

            try {
                dir.getParent().createDirectorySystem(dir.getName());
            } catch (FileExistsException e) {
                // ignore...directory now exists
                logger.debug("Directory {} already exists, not an error", dir.getName());
            } catch (FileNotFoundException e) {
                logger.debug("We received a FileNotFoundException and treating it as an error, returning false");
                return false;
            }
        }

        return true;
    }

    /*
     * This will move the release to the folder specified
     * It will check if it exists, if not it will create all its parent directories
     */
    public boolean moveRelease(DirectoryHandle fromDir) {
        if (_moveRelease) {
            if (!_archiveToFolder.exists()) {
                // dir doesn't exist.  Lets check if Parent does
                if (!createDirectories(_archiveToFolder.getParent())) {
                    logger.warn("Cannot Archive '{}' to '{} : Failed to create Parent Directories", getDirectory().getPath(), _archiveToFolder.getPath());
                    return false;
                }

                // Now that all parents are there...create the new dir
                try {
                    _archiveToFolder.getParent().createDirectorySystem(_archiveToFolder.getName());
                } catch (FileExistsException e) {
                    // ignore...directory now exists
                    logger.debug("Directory {} already exists, not an error", _archiveToFolder.getName());
                } catch (FileNotFoundException e) {
                    logger.warn("Cannot Archive '{}' to '{} unable to create '{}'", getDirectory().getPath(), _archiveToFolder.getPath(), _archiveToFolder.getPath());
                    return false;
                }
            }

            // Check if we need to add another extension to the destination folder
            String type = getDirType(_archiveDirType, fromDir);
            if (!_archiveDirType.isEmpty()) {
                if (type != null) {
                    DirectoryHandle typeInode = new DirectoryHandle(_archiveToFolder.getPath() + VirtualFileSystem.separator + type);
                    if (!typeInode.exists()) {

                        // dir doesn't exist.  Lets check if Parent does
                        if (!createDirectories(typeInode.getParent())) {
                            logger.warn("Cannot Archive '{}' to '{} : Failed to create Parent Directories", getDirectory().getPath(), _archiveToFolder.getPath());
                            return false;
                        }

                        // Now that all parents are there...create the new dir
                        try {
                            typeInode.getParent().createDirectorySystem(typeInode.getName());
                        } catch (FileExistsException e) {
                            // ignore...directory now exists
                            logger.debug("Directory {} already exists, not an error", _archiveToFolder.getName());
                        } catch (FileNotFoundException e) {
                            logger.warn("Cannot Archive '{}' to '{} unable to create dir type '{}'", getDirectory().getPath(), _archiveToFolder.getPath(), typeInode.getPath());
                            return false;
                        }
                    }
                }
            }

            try {
                DirectoryHandle toInode;
                if (type != null) {
                    String toDir = null;
                    if (_archiveDirType.startsWith("rls:")) {
                        /*
                         * Here we will fix ${rls} and "icorrect issue"
                         * toDir may not be same format as first created dir, hence we want to read the
                         * vfs archive dir instead of relying on content in 'type'
                         */

                        String[] paths = type.split("/");
                        String maindir = paths[0];
                        String season = null;
                        if (paths.length > 1) {
                            season = paths[1];
                        }

                        DirectoryHandle inode = new DirectoryHandle(_archiveToFolder.getPath());
                        try {
                            toDir = inode.getDirectoryUnchecked(_archiveToFolder.getPath() + VirtualFileSystem.separator + maindir).getPath();

                            if (season != null) {
                                toDir = toDir + VirtualFileSystem.separator + season;
                            }

                        } catch (FileNotFoundException e) {
                            logger.error("Failed getting DirectoryHandle for somedir [FileNotFoundException]", e);
                        } catch (ObjectNotValidException e) {
                            logger.error("Failed getting DirectoryHandle for somedir [ObjectNotValidException]", e);
                        }
                    }

                    if (toDir != null) {
                        toInode = new DirectoryHandle(toDir + VirtualFileSystem.separator + fromDir.getName());
                    } else {
                        toInode = new DirectoryHandle(_archiveToFolder.getPath() + VirtualFileSystem.separator + type + VirtualFileSystem.separator + fromDir.getName());
                    }
                } else {
                    toInode = new DirectoryHandle(_archiveToFolder.getPath() + VirtualFileSystem.separator + fromDir.getName());
                }

                // Copy modifiedTime
                long time = fromDir.lastModified();
                fromDir.renameToUnchecked(toInode);
                toInode.setLastModified(time);

                _destinationDirectory = toInode;
                return true;

            } catch (FileExistsException e) {
                logger.warn("Cannot Archive '{}' to '{} because it already exists at destination", getDirectory().getPath(), _archiveToFolder.getPath());
            } catch (FileNotFoundException e) {
                logger.warn("Cannot Archive '{}' to '{} because '{}' no longer exists", getDirectory().getPath(), _archiveToFolder.getPath(), getDirectory().getPath());
            }
        } else {
            // No need to move directory, so lets return true
            logger.debug("No need to move directory is our conclusion, so we return true here");
            return true;
        }
        return false;
    }

    /*
     * This is used to get the actual dir that the archived dir needs to be moved too
     * This is overridable to other types can be made within different archive types
     */
    protected String getDirType(String type, DirectoryHandle inode) {
        if (type.equals("alpha")) {
            if (inode.getName().matches("^[0-9].*$")) {
                return "0-9";
            } else if (inode.getName().matches("^[a-zA-Z].*$")) {
                return "" + inode.getName().toUpperCase().charAt(0);
            } else {
                return "UNKNOWN";
            }
        } else if (type.toLowerCase().startsWith("dated:")) {
            String splittype = type.substring(6);
            if (splittype.length() > 1) {
                SimpleDateFormat _dateFormat = new SimpleDateFormat(splittype);
                return _dateFormat.format(new Date());
            }
            logger.warn("Date format invalid for: _archiveDirType");
        } else if (type.toLowerCase().startsWith("rls:")) {
            String splittype = type.substring(4);
            if (splittype.length() > 1) {
                String retstr = splittype;
                if (retstr.contains("${rls}")) {
                    Pattern pattern = Pattern.compile("(?i)(.*)(\\.|-|_)S\\d.*");
                    Matcher matcher = pattern.matcher(inode.getName());
                    if (matcher.matches()) {
                        retstr = retstr.replaceAll("\\$\\{rls\\}", matcher.group(1));
                    }
                }
                if (retstr.contains("${rls}")) {
                    return "UNKNOWN";
                }
                if (retstr.contains("${season}")) {
                    Pattern pattern = Pattern.compile("(?i)(.*\\.|\\-|_)s(\\d|\\d\\d)((\\.|\\-|_)(e|d)|(e|d)).*");
                    Matcher matcher = pattern.matcher(inode.getName());
                    if (matcher.matches()) {
                        if (matcher.group(2).length() < 2) {
                            retstr = retstr.replaceAll("\\$\\{season\\}", 0 + matcher.group(2));
                        } else {
                            retstr = retstr.replaceAll("\\$\\{season\\}", matcher.group(2));
                        }
                    }
                }
                if (retstr.contains("${season}")) {
                    return "UNKNOWN";
                }
                if (retstr.contains("${episode}")) {
                    Pattern pattern = Pattern.compile("(?i)(.*\\.|\\-|_)s\\d.*e(\\d|\\d\\d)(\\.|\\-|_).*");
                    Matcher matcher = pattern.matcher(inode.getName());
                    if (matcher.matches()) {
                        if (matcher.group(2).length() < 2) {
                            retstr = retstr.replaceAll("\\$\\{season\\}", 0 + matcher.group(2));
                        } else {
                            retstr = retstr.replaceAll("\\$\\{season\\}", matcher.group(2));
                        }
                    }
                }
                if (retstr.contains("${episode}")) {
                    return "UNKNOWN";
                }

                while (retstr.contains("${regex:")) {
                    String regexstr = retstr.substring(retstr.indexOf("${regex:") + 8, retstr.indexOf("}"));
                    if (regexstr.length() < 1) {
                        break;
                    }
                    String fullstr = "${regex:" + regexstr + "}";
                    try {
                        Pattern pattern = Pattern.compile(regexstr);
                        Matcher matcher = pattern.matcher(inode.getName());
                        if (matcher.matches()) {
                            retstr = retstr.replace(fullstr, matcher.group(1));
                        }
                    } catch (PatternSyntaxException e) {
                        logger.error("Regex Syntax Error in '{}' for '{}'", regexstr, fullstr, e);
                    }

                    if (retstr.contains(fullstr)) {
                        return "UNKNOWN";
                    }

                }
                return retstr;
            }
            return "UNKNOWN";
        } else if (type.toLowerCase().startsWith("regex:")) {
            String splittype = type.substring(6);
            if (splittype.length() > 1) {
                Pattern pattern = Pattern.compile(splittype);
                Matcher matcher = pattern.matcher(inode.getName());
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
            return "UNKNOWN";
        }

        if (!type.equals("")) {
            logger.warn("No valid type found for: {}", type);
        }

        return null;
    }

    /*
     * Returns if we are just moving the directory and not archive to other slaves
     */
    public boolean moveReleaseOnly() {
        return _moveReleaseOnly;
    }

    /*
     * Returns if the release is moving to a different dir
     */
    public boolean isMovingRelease() {
        return _moveRelease;
    }

    /*
     * returns the destination directory (including type) after move (mostly for events)
     */
    public DirectoryHandle getDestinationDirectory() {
        return _destinationDirectory;
    }

    /*
     * Adds failed dir for archiving when using repeat
     */
    public void addFailedDir(String path) {
        _failedDirs.add(path);
    }

    /*
     * Returns if dir has been added to the Failed dirs
     */
    public boolean checkFailedDir(String path) {
        return _failedDirs.contains(path);
    }

}
