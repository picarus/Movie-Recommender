package mai.cn.film.domain;

public abstract class Rate implements GraphNode {
	
	int idGraph;
	
	public abstract int getId();
	public abstract int getRate();
	
	Rate(){
		idGraph=-1;
	}
	
	@Override
	public void setIdxNode(int idx) {
		idGraph=idx;
	}

	@Override
	public int getIdxNode() {
		return idGraph;
	}
}
