package mai.cn.film.domain;

public class Affinity implements Comparable<Affinity> {
	double metric;
	int id1,id2;
	Type type;
	int label1,label2;
	
	public Affinity(int id1,int label1, int id2, int label2, double metric, Type type){
		this.id1=id1;
		this.label1=label1;
		this.id2=id2;
		this.label2=label2;
		this.metric=metric;
		this.type=type;
	}
	
	public int getOtherLabel(int label){
		if (label1!=label) return label1;
		else return label2;
	}
	
	public double getMetric() {
		return metric;
	}
	
	public void setMetric(double metric) {
		this.metric = metric;
	}
	
	public int getId1() {
		return id1;
	}
	
	public void setId1(int id1) {
		this.id1 = id1;
	}
	
	public int getId2() {
		return id2;
	}
	
	public void setId2(int id2) {
		this.id2 = id2;
	}

	public Type getType() {
		return type;
	}

	public int getLabel1() {
		return label1;
	}

	public int getLabel2() {
		return label2;
	}

	@Override
	public int compareTo(Affinity aff) {
		Double m=aff.getMetric();
		return Double.compare(metric, m);
	}

}
