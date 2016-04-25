package org.ssase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssase.model.sam.ModelFunctionConfigGenerator;
import org.ssase.objective.Cost;
import org.ssase.objective.Objective;
import org.ssase.objective.QualityOfService;
import org.ssase.primitive.ControlPrimitive;
import org.ssase.primitive.Primitive;
import org.ssase.primitive.SoftwareControlPrimitive;
import org.ssase.util.Repository;
/**
 * This is actually a service-instance.
 * @author tao
 *
 */
public class Service {

	protected static final Logger logger = LoggerFactory
	.getLogger(Service.class);
	// String name - QoS/Cost
	private Map<String, Objective> objectives;
	// String name - software CP/EP, these are all direct primitive, not including the functional dependent ones.
	// This does not include the shared software primitives.
	private Map<String, Primitive> primitives;

	private Set<Primitive> possibleInputs = new HashSet<Primitive>();
	
	// Functionally dependent service, can use this to get their primitives.
	private Set<Service> dependentServices;
	
	private String VM_ID;
	
	
	private String name;
	
	@Deprecated
	public Service(){
		
	}
	
	public Service( 
			String vM_ID, 
			String name,
			Map<String, Objective> objectives /*The objective here is with a null Model instance*/,
			Map<String, Primitive> primitives) {
		super();
		this.objectives = objectives;
		this.primitives = primitives;
		VM_ID = vM_ID;
		this.name = name;
	}
	
	public void addPossiblePrimitive (Collection<Primitive> p) {
		possibleInputs.addAll(p);
	}
	
	public void removePossiblePrimitive (Collection<Primitive> p) {
		possibleInputs.removeAll(p);
	}
	
	public void addPossiblePrimitive (Primitive p) {
		possibleInputs.add(p);
	}
	
	public void removePossiblePrimitive (Primitive p) {
		possibleInputs.remove(p);
	}
	 
	public void initializeModelForQoS(){
		for (Objective obj : objectives.values()) {
			if(obj instanceof QualityOfService) {
				QualityOfService qos = (QualityOfService)obj;
				/**
				 * 
				 * We use SAM (Sensitivity Aware Model) models by default
				 * */
				qos.buildModel(
						possibleInputs, 
						ModelFunctionConfigGenerator.getFunctionConfiguration(obj.getName()), 
						ModelFunctionConfigGenerator.getStructureConfiguration(obj.getName()));
			}
		}
	}

	public void prepareToUpdatePrimitiveValue(String name, double... values){
		if (!primitives.containsKey(name)) {
			return;
		}
		
		for (double v : values) {
			primitives.get(name).prepareToAddValue(v) ;
		} 
	}
	
	public void prepareToUpdateQoSValue(String name, double... values){	
		for (double v : values) {
			((QualityOfService)objectives.get(name)).prepareToAddValue(v) ;
		} 
	}
	
	public Collection<Objective> getObjectives(){
		return objectives.values();
	}
	
	public Collection<Primitive> getPrimitives(){
		return primitives.values();
	}
	
	public Primitive getPrimitive(String name){
		return primitives.get(name);
	}
	
	public Set<Primitive> getPossiblePrimitives(){
		return possibleInputs;
	}
	
	public Objective getObjective(String name){
		return objectives.get(name);
	}
	
	public void addObjective(String s, Objective obj){
		objectives.put(s, obj);
	}
	
	public List<Primitive> getSoftwarePrimitives(){
		List<Primitive> result = new ArrayList<Primitive>();
		for (Map.Entry<String, Primitive> e : primitives.entrySet()) {
			if (e.getValue() instanceof SoftwareControlPrimitive) {
				result.add(e.getValue());
			}
		}
		
		return result;
	}
	
	public String getName(){
		return name;
	}
	
	
	public String getVMID(){
		return VM_ID;
	}
	
	public boolean isHasObjectiveToModel(){
		return objectives != null && objectives.size() != 0;
	}
	
	public void updateCostModelInputs (){
		Cost cost = null;
		List<Primitive> list = new ArrayList<Primitive>();
		for (Map.Entry<String, Objective> e : objectives.entrySet()) {
			if (e.getValue() instanceof QualityOfService) {
				for (Primitive p : e.getValue().getPrimitivesInput()) {
					if (p instanceof ControlPrimitive && primitives.containsValue(p) && 
							!list.contains(p)) {
						list.add(p);
					}
				}
			} else {
				cost = (Cost) e.getValue();
			}
		}
		
		
		cost.setInputs(list);
		
		
	}
	
	public void print(){
		logger.debug(name + " \n");
		logger.debug("Objective: \n");
		for (Map.Entry<String, Objective> en : objectives.entrySet()) {
			logger.debug("--------" + en.getKey() + "," + en.getValue().getName() + ":" +  en.getValue() + " \n");
			logger.debug("Direct Primitives: \n");
			for (Primitive p : possibleInputs) {
				if (Repository.isDirectForAnObjective(en.getValue(), p)) {
					logger.debug(p.getName() + "=" + p.getAlias()  + ":" +  p + " \n");
				}
				
			}
		}
		logger.debug(possibleInputs.size() + " Possible Primitives: \n");
		for (Primitive p : possibleInputs) {
			logger.debug(p.getName() + "=" + p.getAlias()  + ":" +  p + " \n");
		}
	
	}
	
}
