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
package org.drftpd.nukefilter.master;

import java.util.ArrayList;

/**
 * @author phew
 */
public class NukeFilterSectionConfig implements NukeFilterConfigInterface {

    private Integer nukeDelay;
    private Integer enforceYearNukex;
    private Integer enforceGroupNukex;
    private final ArrayList<NukeFilterConfigElement> filterString;
    private final ArrayList<NukeFilterConfigElement> enforceString;
    private final ArrayList<NukeFilterConfigElement> filterRegex;
    private final ArrayList<NukeFilterConfigElement> enforceRegex;
    private final ArrayList<NukeFilterConfigElement> filterYear;
    private final ArrayList<NukeFilterConfigElement> enforceYear;
    private final ArrayList<NukeFilterConfigElement> filterGroup;
    private final ArrayList<NukeFilterConfigElement> enforceGroup;

    public NukeFilterSectionConfig() {
        nukeDelay = 120;
        enforceYearNukex = 3;
        enforceGroupNukex = 3;
        filterString = new ArrayList<>();
        enforceString = new ArrayList<>();
        filterRegex = new ArrayList<>();
        enforceRegex = new ArrayList<>();
        filterYear = new ArrayList<>();
        enforceYear = new ArrayList<>();
        filterGroup = new ArrayList<>();
        enforceGroup = new ArrayList<>();
    }

    public void addEnforceGroupElement(NukeFilterConfigElement element) {
        enforceGroup.add(element);
    }

    public void addEnforceRegexElement(NukeFilterConfigElement element) {
        enforceRegex.add(element);
    }

    public void addEnforceStringElement(NukeFilterConfigElement element) {
        enforceString.add(element);
    }

    public void addEnforceYearElement(NukeFilterConfigElement element) {
        enforceYear.add(element);
    }

    public void addFilterGroupElement(NukeFilterConfigElement element) {
        filterGroup.add(element);
    }

    public void addFilterRegexElement(NukeFilterConfigElement element) {
        filterRegex.add(element);
    }

    public void addFilterStringElement(NukeFilterConfigElement element) {
        filterString.add(element);
    }

    public void addFilterYearElement(NukeFilterConfigElement element) {
        filterYear.add(element);
    }

    public ArrayList<NukeFilterConfigElement> getEnforceGroupList() {
        return enforceGroup;
    }

    public ArrayList<NukeFilterConfigElement> getEnforceRegexList() {
        return enforceRegex;
    }

    public ArrayList<NukeFilterConfigElement> getEnforceStringList() {
        return enforceString;
    }

    public ArrayList<NukeFilterConfigElement> getEnforceYearList() {
        return enforceYear;
    }

    public ArrayList<NukeFilterConfigElement> getFilterGroupList() {
        return filterGroup;
    }

    public ArrayList<NukeFilterConfigElement> getFilterRegexList() {
        return filterRegex;
    }

    public ArrayList<NukeFilterConfigElement> getFilterStringList() {
        return filterString;
    }

    public ArrayList<NukeFilterConfigElement> getFilterYearList() {
        return filterYear;
    }

    public Integer getNukeDelay() {
        return nukeDelay;
    }

    public void setNukeDelay(Integer delay) {
        nukeDelay = delay;
    }

    public boolean hasEnforceGroups() {
        return !enforceGroup.isEmpty();
    }

    public boolean hasEnforceRegex() {
        return !enforceRegex.isEmpty();
    }

    public boolean hasEnforceStrings() {
        return !enforceString.isEmpty();
    }

    public boolean hasEnforceYears() {
        return !enforceYear.isEmpty();
    }

    public boolean hasFilterGroups() {
        return !filterGroup.isEmpty();
    }

    public boolean hasFilterRegex() {
        return !filterRegex.isEmpty();
    }

    public boolean hasFilterStrings() {
        return !filterString.isEmpty();
    }

    public boolean hasFilterYears() {
        return !filterYear.isEmpty();
    }

    public Integer getEnforceYearNukex() {
        return enforceYearNukex;
    }

    public void setEnforceYearNukex(Integer nukex) {
        enforceYearNukex = nukex;
    }

    public Integer getEnforceGroupNukex() {
        return enforceGroupNukex;
    }

    public void setEnforceGroupNukex(Integer nukex) {
        enforceGroupNukex = nukex;
    }

}
