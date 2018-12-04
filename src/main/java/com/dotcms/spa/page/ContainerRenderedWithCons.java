package com.dotcms.spa.page;

import java.util.Map;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRendered;

/**
 * Represents the information of the {@link Container} and its respective
 * {@link ContainerStructure} relationships. These relationships define what Content Types can be
 * added to the container.
 *
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
public class ContainerRenderedWithCons extends  ContainerRendered {

    private static final long serialVersionUID = 1572918359580445566L;

    private final Map<String,Object> contentlets;
    /**
     * Creates a new instance of the ContainerRendered.
     *
     * @param container           The {@link Container} in the HTML Page.
     * @param containerStructures The list of {@link ContainerStructure} relationships.
     *                           the browser.
     */
    public ContainerRenderedWithCons(ContainerRendered containerRendered, Map<String,Object> contentlets) {
        super(containerRendered.getContainer(), containerRendered.getContainerStructures(),containerRendered.getRendered());
        
        
        
        
        this.contentlets = contentlets;
    }


}
