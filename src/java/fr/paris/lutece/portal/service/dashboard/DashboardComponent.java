/*
 * Copyright (c) 2002-2008, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.portal.service.dashboard;

import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;

/**
 * Dashboard Component
 */
public abstract class DashboardComponent implements Comparable
{

    private String _strName;
    private String _strRight;
    private int _nZone;
    private int _nOrder;
    private Plugin _plugin;

    /**
     * Gets dashboard data for a given user
     * @param user The user
     * @return HTML content to insert into a dashboard zone
     */
    public abstract String getDashboardData( AdminUser user );

    /**
     * Returns the Name
     * @return The Name
     */
    public String getName()
    {
        return _strName;
    }

    /**
     * Sets the Name
     * @param strName The Name
     */
    public void setName( String strName )
    {
        _strName = strName;
    }

    /**
     * Returns the PluginName
     *
     * @return The PluginName
     */
    /**
     * Returns the Right
     * @return The Right
     */
    public String getRight()
    {
        return _strRight;
    }

    /**
     * Sets the Right
     * @param strRight The Right
     */
    public void setRight( String strRight )
    {
        _strRight = strRight;
    }

    /**
     * Returns the Zone
     * @return The Zone
     */
    public int getZone()
    {
        return _nZone;
    }

    /**
     * Sets the Zone
     * @param nZone The Zone
     */
    public void setZone( int nZone )
    {
        _nZone = nZone;
    }

    /**
     * Returns the Order
     * @return The Order
     */
    public int getOrder()
    {
        return _nOrder;
    }

    /**
     * Sets the Order
     * @param nOrder The Order
     */
    public void setOrder( int nOrder )
    {
        _nOrder = nOrder;
    }

    /**
     * Returns the Plugin
     * @return The Plugin
     */
    public Plugin getPlugin()
    {
        return _plugin;
    }

    /**
     * Sets the Plugin
     * @param plugin The plugin
     */
    public void setPlugin( Plugin plugin )
    {
        _plugin = plugin;
    }

    /**
     * Compare component order
     * @param o The component to compare to
     * @return less than 0 if the order is lower, 0 if equals and greater than 0 if higher
     */
    public int compareTo( Object o )
    {
        return getOrder() - (( DashboardComponent ) o).getOrder();
    }
    
    /**
     * Tells if the component is enabled
     * @return true if enabled
     */
    public boolean isEnabled()
    {
        return PluginService.isPluginEnable( _plugin.getName() );
    }
}