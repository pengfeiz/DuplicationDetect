/*
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yottabox.hbasemodel;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a list of table regions.
 * <p/>
 * <pre>
 * &lt;complexType name="TableInfo"&gt;
 *   &lt;sequence&gt;
 *     &lt;element name="region" type="tns:TableRegion"
 *       maxOccurs="unbounded" minOccurs="1"&gt;&lt;/element&gt;
 *   &lt;/sequence&gt;
 *   &lt;attribute name="name" type="string"&gt;&lt;/attribute&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlRootElement(name = "TableInfo")
@XmlType(propOrder = {"name", "regions"})
public class TableInfoModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private List<TableRegionModel> regions = new ArrayList<TableRegionModel>();

    /**
     * Default constructor
     */
    public TableInfoModel() {
    }

    /**
     * Constructor
     *
     * @param name
     */
    public TableInfoModel(String name) {
        this.name = name;
    }

    /**
     * Add a region model to the list
     *
     * @param region the region
     */
    public void add(TableRegionModel region) {
        regions.add(region);
    }

    /**
     * @param index the index
     * @return the region model
     */
    public TableRegionModel get(int index) {
        return regions.get(index);
    }

    /**
     * @return the table name
     */
    @XmlAttribute
    public String getName() {
        return name;
    }

    /**
     * @return the regions
     */
    @XmlElement(name = "Region")
    public List<TableRegionModel> getRegions() {
        return regions;
    }

    /**
     * @param name the table name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param regions the regions to set
     */
    public void setRegions(List<TableRegionModel> regions) {
        this.regions = regions;
    }

    /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TableRegionModel aRegion : regions) {
            sb.append(aRegion.toString());
            sb.append('\n');
        }
        return sb.toString();
    }


}
