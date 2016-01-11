/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package frostwire.search.archiveorg;

import java.util.List;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class ArchiveorgItem {

    public String title;
    public String mediatype;
    public String description;
    public String licenseurl;
    public String publicdate;
    public int downloads;
    public int week;
    public int month;
    public int num_reviews;
    public float avg_rating;
    public String identifier;
    public List<String> format;
    public List<String> collection;
}
