/*
 * This file is part of ContestAdmin. ContestAdmin is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version. ContestAdmin is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received
 * a copy of the GNU General Public License along with ContestAdmin. If not, see <http://www.gnu.org/licenses/>.
 */
package sk.tomsik68.contestadmn;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.avaje.ebean.validation.NotNull;
/** Class that holds Contest Entry in database.
 * 
 * @author Tomsik68
 *
 */
@Entity
@Table(name="contest_entries")
public class ContestEntry {
    @NotNull
    @Id
    private String playerName;
    @NotNull
    private String contestName;
    @NotNull
    private double x,y,z;
    @NotNull
    private String worldName;
    public ContestEntry(){
        
    }
    public Location getLocation(){
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }
    public String getPlayerName() {
        return playerName;
    }
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    public String getContestName() {
        return contestName;
    }
    public void setContestName(String contestName) {
        this.contestName = contestName;
    }
    public double getX() {
        return x;
    }
    public void setX(double x) {
        this.x = x;
    }
    public double getY() {
        return y;
    }
    public void setY(double y) {
        this.y = y;
    }
    public double getZ() {
        return z;
    }
    public void setZ(double z) {
        this.z = z;
    }
    public String getWorldName() {
        return worldName;
    }
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
}
