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

import com.avaje.ebean.validation.NotNull;
/** Class for contests
 * 
 * @author Tomsik68
 *
 */
@Entity
@Table(name="contests")
public class Contest {
    @Id
    @NotNull
    private String name;
    @NotNull
    private String starter;
    
    private String description;
    private String rules;
    private String bannedUsers;
    private boolean ended;
    private String winner;
    public Contest(){
        //Ebean constructor
    }
    public Contest(String nam, String star){
        setName(nam);
        setEnded(false);
        //standard contructor
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getStarter() {
        return starter;
    }
    public void setStarter(String starter) {
        this.starter = starter;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getRules() {
        return rules;
    }
    public void setRules(String rules) {
        this.rules = rules;
    }
    public String getBannedUsers() {
        return bannedUsers;
    }
    public void setBannedUsers(String bannedUsers) {
        this.bannedUsers = bannedUsers;
    }
    public boolean getEnded() {
        return ended;
    }
    public void setEnded(boolean ended) {
        this.ended = ended;
    }
    public String getWinner() {
        return winner;
    }
    public void setWinner(String winner) {
        this.winner = winner;
    }
}
