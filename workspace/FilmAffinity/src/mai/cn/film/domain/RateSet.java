package mai.cn.film.domain;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.Vector;

public abstract class RateSet /*implements GraphNode*/ {
	
	public abstract SortedMap<Integer,? extends Rate> getRates();
	public abstract int getId();
	
	float avgRate;
	float totalRate;
	Vector<Affinity> affinities;
	
	public RateSet(){
		avgRate=0.0f;
		totalRate=0.0f;
		affinities=new Vector<Affinity>();
	}
	
	public void updateAvgRate(float rate, int size){
		totalRate+=rate;
		avgRate=totalRate/size;
	}
	
	public float getAvgRate() {
		return avgRate;
	}
	
	public abstract int numberOfRates();
	
	public Iterator<Affinity> getAffinities() {
		return affinities.iterator();
	}
	
	public void addAffinity(Affinity aff){
		affinities.add(aff);
	}
	
}
