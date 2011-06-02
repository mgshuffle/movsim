/**
 * Copyright (C) 2010, 2011 by Arne Kesting, Martin Treiber,
 *                             Ralph Germ, Martin Budden
 *                             <info@movsim.org>
 * ----------------------------------------------------------------------
 * 
 *  This file is part of 
 *  
 *  MovSim - the multi-model open-source vehicular-traffic simulator 
 *
 *  MovSim is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MovSim is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MovSim.  If not, see <http://www.gnu.org/licenses/> or
 *  <http://www.movsim.org>.
 *  
 * ----------------------------------------------------------------------
 */
package org.movsim.input.model.vehicle.longModel.impl;

import org.movsim.input.model.vehicle.longModel.AccelerationModelInputData;
import org.movsim.utilities.ObservableInTime;
import org.movsim.utilities.impl.ObservableImpl;

// TODO: Auto-generated Javadoc
/**
 * The Class AccelerationModelInputDataImpl.
 */
public abstract class AccelerationModelInputDataImpl extends ObservableImpl implements AccelerationModelInputData {

    /** The model name. */
    private final String modelName;
    
    public abstract void resetParametersToDefault();
    
    protected abstract void checkParameters();
    
    protected void parametersUpdated(){
        checkParameters();
        notifyObservers();
    }

    /**
     * Instantiates a new model input data impl.
     * 
     * @param modelName
     *            the model name
     */
    public AccelerationModelInputDataImpl(String modelName) {
        this.modelName = modelName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.movsim.input.model.vehicle.longModel.AccelerationModelInputData#getModelName()
     */
    @Override
    public String getModelName() {
        return modelName;
    }

}
