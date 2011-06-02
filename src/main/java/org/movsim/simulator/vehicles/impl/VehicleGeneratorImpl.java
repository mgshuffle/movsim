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
package org.movsim.simulator.vehicles.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.movsim.input.InputData;
import org.movsim.input.model.VehicleInput;
import org.movsim.input.model.simulation.HeterogeneityInputData;
import org.movsim.input.model.vehicle.longModel.AccelerationModelInputData;
import org.movsim.input.model.vehicle.longModel.AccelerationModelInputDataACC;
import org.movsim.input.model.vehicle.longModel.AccelerationModelInputDataGipps;
import org.movsim.input.model.vehicle.longModel.AccelerationModelInputDataIDM;
import org.movsim.input.model.vehicle.longModel.AccelerationModelInputDataKCA;
import org.movsim.input.model.vehicle.longModel.AccelerationModelInputDataNSM;
import org.movsim.input.model.vehicle.longModel.AccelerationModelInputDataNewell;
import org.movsim.input.model.vehicle.longModel.AccelerationModelInputDataOVM_VDIFF;
import org.movsim.simulator.Constants;
import org.movsim.simulator.impl.MyRandom;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.simulator.vehicles.VehicleGenerator;
import org.movsim.simulator.vehicles.VehiclePrototype;
import org.movsim.simulator.vehicles.longmodel.accelerationmodels.AccelerationModel;
import org.movsim.simulator.vehicles.longmodel.accelerationmodels.impl.ACC;
import org.movsim.simulator.vehicles.longmodel.accelerationmodels.impl.Gipps;
import org.movsim.simulator.vehicles.longmodel.accelerationmodels.impl.IDM;
import org.movsim.simulator.vehicles.longmodel.accelerationmodels.impl.KCA;
import org.movsim.simulator.vehicles.longmodel.accelerationmodels.impl.NSM;
import org.movsim.simulator.vehicles.longmodel.accelerationmodels.impl.Newell;
import org.movsim.simulator.vehicles.longmodel.accelerationmodels.impl.OVM_VDIFF;
import org.movsim.simulator.vehicles.longmodel.equilibrium.EquilibriumProperties;
import org.movsim.simulator.vehicles.longmodel.equilibrium.impl.EquilibriumACC;
import org.movsim.simulator.vehicles.longmodel.equilibrium.impl.EquilibriumGipps;
import org.movsim.simulator.vehicles.longmodel.equilibrium.impl.EquilibriumIDM;
import org.movsim.simulator.vehicles.longmodel.equilibrium.impl.EquilibriumKCA;
import org.movsim.simulator.vehicles.longmodel.equilibrium.impl.EquilibriumNSM;
import org.movsim.simulator.vehicles.longmodel.equilibrium.impl.EquilibriumNewell;
import org.movsim.simulator.vehicles.longmodel.equilibrium.impl.EquilibriumOVM_VDIFF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * The Class VehicleGeneratorImpl.
 */
public class VehicleGeneratorImpl implements VehicleGenerator {
    
    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(VehicleGeneratorImpl.class);

    // Aufwand mit prototypes wg. einmaliger berechnung des FD
    // Und ggf. einmaliger Neuberechnung FD nach Parameteraenderung !!

    /** The project name. */
    private final String projectName;

    // enthaelt die Menge der definierte Models ... notwendig fuer GUI
    // private HashMap<String, AccelerationModel> longModels;

    // enthaelt die Heterogenitaet der tatsaechlich simulierten Fahrzeuge
    /** The prototypes. */
    private final HashMap<String, VehiclePrototype> prototypes;

    /** The required timestep. */
    private double requiredTimestep;

    /** The is with reaction times. */
    private final boolean isWithReactionTimes;

    /**
     * Instantiates a new vehicle generator impl.
     * 
     * @param isWithGUI
     *            the is with gui
     * @param simInput
     *            the sim input
     */
    public VehicleGeneratorImpl(boolean isWithGUI, InputData simInput) {

        this.projectName = simInput.getProjectName();

        // erzeuge long models:
        // longModels = new HashMap<String, AccelerationModel>();
        // createLongModels(simInput);

        // erzeuge vehicle prototypen gemaess heterogeneity
        prototypes = new HashMap<String, VehiclePrototype>();
        final double sumFraction = createPrototypes(simInput);

        // normalize heterogeneity fractions
        normalizeFractions(sumFraction);

        // output fundamental diagrams
        if (!isWithGUI && simInput.getSimulationInput().getSingleRoadInput().isWithWriteFundamentalDiagrams()) {
            writeFundamentalDiagrams();
        }

        isWithReactionTimes = checkForReactionTimes();

    }

    /**
     * Creates the prototypes.
     * 
     * @param simInput
     *            the sim input
     * @return the double
     */
    private double createPrototypes(InputData simInput) {

        // default for continuous micro models
        requiredTimestep = simInput.getSimulationInput().getTimestep(); 

        final Map<String, VehicleInput> vehInputMap = createMap(simInput.getVehicleInputData());

        final List<HeterogeneityInputData> heterogenInputData = simInput.getSimulationInput().getSingleRoadInput()
                .getHeterogeneityInputData();

        double sumFraction = 0;
        for (final HeterogeneityInputData heterogen : heterogenInputData) {
            final String keyName = heterogen.getKeyName();
            logger.debug("key name={}", keyName);
            if (!vehInputMap.containsKey(keyName)) {
                logger.info("no corresponding vehicle found. Ignore heterogeneity with label={}", keyName);
                continue;
            }
            final VehicleInput vehInput = vehInputMap.get(keyName);

            final double vehLength = vehInput.getLength();
            final AccelerationModel longModel = longModelFactory(vehInput.getModelInputData(), vehLength);

            final EquilibriumProperties fundDia = fundDiagramFactory(vehLength, longModel);

            final double fraction = heterogen.getFraction();
            logger.info("fraction = {}", fraction);

            // TODO check logic: prototypes (e.g. obstacles) needed for IC but
            // with fraction == 0
            // if (fraction > 0) {
            sumFraction += fraction;
            final VehiclePrototype vehProto = new VehiclePrototype(keyName, fraction, longModel, fundDia, vehInput);
            prototypes.put(keyName, vehProto);

            // set simulation update time here from model classes:
            final double requiredTimestepLocal = longModel.getRequiredUpdateTime();
            if (requiredTimestepLocal > Constants.SMALL_VALUE) {
                if (Math.abs(requiredTimestepLocal - requiredTimestep) > Constants.SMALL_VALUE) {
                    logger.error(
                            "inconsistent model input: cannot simulate these models with incompatible update times dtModel={} and dtSim={}",
                            requiredTimestepLocal, requiredTimestep);
                    System.exit(-1);
                } else {
                    requiredTimestep = requiredTimestepLocal;
                    logger.info("set simulation timestep to dt={} for model = {}", requiredTimestep,
                            longModel.modelName());
                }
            }
            logger.debug("simulation timestep: dt={}. ", requiredTimestep);
            // }

        }
        return sumFraction;
    }

    /**
     * Creates the map.
     * 
     * @param vehicleInputData
     *            the vehicle input data
     * @return the map
     */
    
    // TODO auslagern auf Input seite
    private Map<String, VehicleInput> createMap(List<VehicleInput> vehicleInputData) {
        final HashMap<String, VehicleInput> map = new HashMap<String, VehicleInput>();
        for (final VehicleInput vehInput : vehicleInputData) {
            final String keyName = vehInput.getLabel();
            map.put(keyName, vehInput);
        }
        return map;
    }

    /**
     * Fund diagram factory.
     * 
     * @param vehLength
     *            the veh length
     * @param longModel
     *            the long model
     * @return the equilibrium properties
     */
    private EquilibriumProperties fundDiagramFactory(double vehLength, AccelerationModel longModel) {
        if (longModel.modelName().equalsIgnoreCase(Constants.MODEL_NAME_IDM))
            return new EquilibriumIDM(vehLength, (IDM) longModel);
        else if (longModel.modelName().equalsIgnoreCase(Constants.MODEL_NAME_ACC))
            return new EquilibriumACC(vehLength, (ACC) longModel);
        else if (longModel.modelName().equalsIgnoreCase(Constants.MODEL_NAME_OVM_VDIFF))
            return new EquilibriumOVM_VDIFF(vehLength, (OVM_VDIFF) longModel);
        else if (longModel.modelName().equalsIgnoreCase(Constants.MODEL_NAME_GIPPS))
            return new EquilibriumGipps(vehLength, (Gipps) longModel);
        else if (longModel.modelName().equalsIgnoreCase(Constants.MODEL_NAME_NEWELL))
            return new EquilibriumNewell(vehLength, (Newell) longModel);
        else if (longModel.modelName().equalsIgnoreCase(Constants.MODEL_NAME_NSM))
            return new EquilibriumNSM(vehLength, (NSM) longModel);
        else if (longModel.modelName().equalsIgnoreCase(Constants.MODEL_NAME_KCA))
            return new EquilibriumKCA(vehLength, (KCA) longModel);
        else {
            logger.error("no fundamental diagram constructed for model {}. exit.", longModel.modelName());
            System.exit(0);
        }
        return null; // not reached after exit !

    }

    /**
     * Long model factory.
     * 
     * @param modelInputData
     *            the model input data
     * @param vehLength
     *            the veh length
     * @return the acceleration model
     */
    private AccelerationModel longModelFactory(AccelerationModelInputData modelInputData, double vehLength) {
        final String modelName = modelInputData.getModelName();
        AccelerationModel longModel = null;
        logger.info("modelName = {}", modelName);
        if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_IDM)) {
            longModel = new IDM(modelName, (AccelerationModelInputDataIDM)modelInputData);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_ACC)) {
            longModel = new ACC(modelName, (AccelerationModelInputDataACC) modelInputData);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_OVM_VDIFF)) {
            longModel = new OVM_VDIFF(modelName, (AccelerationModelInputDataOVM_VDIFF) modelInputData);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_GIPPS)) {
            longModel = new Gipps(modelName, (AccelerationModelInputDataGipps) modelInputData);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_NEWELL))
            return new Newell(modelName, (AccelerationModelInputDataNewell) modelInputData);
        else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_NSM)) {
            longModel = new NSM(modelName, (AccelerationModelInputDataNSM) modelInputData);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_KCA)) {
         // needs vehicle length
            longModel = new KCA(modelName, (AccelerationModelInputDataKCA) modelInputData, vehLength); 
        } else {
            logger.error("create model by inputParameter: Model {} not known !", modelName);
            System.exit(0); // TODO
        }
        return longModel;
    }

    
    
    /**
     * Long model factory.
     * 
     * @param modelToCopy
     *            the model to copy
     * @return the acceleration model
     */
    private AccelerationModel longModelFactory(AccelerationModel modelToCopy) {
        AccelerationModel longModel = null;
        final String modelName = modelToCopy.modelName();
        if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_IDM)) {
            longModel = new IDM((IDM) modelToCopy);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_ACC)) {
            longModel = new ACC((ACC) modelToCopy);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_OVM_VDIFF)) {
            longModel = new OVM_VDIFF((OVM_VDIFF) modelToCopy);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_GIPPS)) {
            longModel = new Gipps((Gipps) modelToCopy);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_NSM)) {
            longModel = new NSM((NSM) modelToCopy);
        } else if (modelName.equalsIgnoreCase(Constants.MODEL_NAME_KCA)) {
            longModel = new KCA((KCA) modelToCopy);
        } else {
            logger.error("create model by copy constructor: Model {} not known ! %n", modelName);
            System.exit(0); // TODO
        }
        return longModel;
    }

    
    
    /**
     * Normalize fractions.
     * 
     * @param sumFraction
     *            the sum fraction
     */
    private void normalizeFractions(double sumFraction) {
        final Iterator<String> it = prototypes.keySet().iterator();
        while (it.hasNext()) {
            final String key = it.next();
            final double fraction = prototypes.get(key).fraction();
            prototypes.get(key).setFraction(fraction / sumFraction);
        }
    }

    /**
     * Write fundamental diagrams.
     */
    
    // TODO: auslagern in separate Klasse 
    private void writeFundamentalDiagrams() {
        final Iterator<String> it = prototypes.keySet().iterator();
        while (it.hasNext()) {
            final String key = it.next();
            final String filename = projectName + ".fund_" + key + ".csv";
            final VehiclePrototype proto = prototypes.get(key);
            if (proto.fraction() > 0) {
                // avoid writing fundDia of "obstacles"
                proto.writeFundamentalDiagram(filename);
            }
        }
    }

    
    /**
     * Check for reaction times.
     * 
     * @return true, if successful
     */
    private boolean checkForReactionTimes() {
        final Iterator<String> it = prototypes.keySet().iterator();
        while (it.hasNext()) {
            final String key = it.next();
            final VehiclePrototype prototype = prototypes.get(key);
            if (prototype.hasReactionTime())
                return true;
        }
        return false;
    }
    
    /**
     * Cyclic buffer factory.
     * 
     * @return the cyclic buffer impl
     */
    
    private CyclicBufferImpl cyclicBufferFactory() {
        if (isWithReactionTimes)
            return new CyclicBufferImpl();
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.vehicles.VehicleGenerator#getVehiclePrototype()
     */
    @Override
    public VehiclePrototype getVehiclePrototype() {
        final double randomNumber = MyRandom.nextDouble();
        double sumFraction = 0;
        final Iterator<String> it = prototypes.keySet().iterator();
        while (it.hasNext()) {
            final String key = it.next();
            sumFraction += prototypes.get(key).fraction();
            if (sumFraction >= randomNumber)
                return prototypes.get(key);
        }
        logger.error("no vehicle prototype found for randomNumber= {}", randomNumber);
        System.exit(-1);
        return null; // not reached after exit
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.movsim.simulator.vehicles.VehicleGenerator#createVehicle(org.movsim
     * .simulator.vehicles.VehiclePrototype)
     */
    @Override
    public Vehicle createVehicle(VehiclePrototype prototype) {
        final int vehID = MyRandom.nextInt(); // for veh index
        // final double length = prototype.length();
        // final double reactionTime = prototype.reactionTime();
        final VehicleInput vehInput = prototype.getVehicleInput();
        final AccelerationModel longModel = longModelFactory(prototype.getLongModel());
        final CyclicBufferImpl cyclicBuffer = cyclicBufferFactory();

        final Vehicle veh = new VehicleImpl(prototype.getLabel(), vehID, longModel, vehInput, cyclicBuffer);
        return veh;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.vehicles.VehicleGenerator#createVehicle()
     */
    @Override
    public Vehicle createVehicle() {
        final VehiclePrototype prototype = getVehiclePrototype();
        return createVehicle(prototype);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.movsim.simulator.vehicles.VehicleGenerator#createVehicle(java.lang
     * .String)
     */
    @Override
    public Vehicle createVehicle(String typeLabel) {
        if (!prototypes.containsKey(typeLabel)) {
            logger.error("cannot create vehicle. label = {} not defined. exit. ", typeLabel);
            System.exit(-1);
        }
        final VehiclePrototype prototype = prototypes.get(typeLabel);
        logger.info("create vehicle with label = {}", typeLabel);
        return createVehicle(prototype);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.simulator.vehicles.VehicleGenerator#requiredTimestep()
     */
    @Override
    public double requiredTimestep() {
        return requiredTimestep;
    }

   

}
