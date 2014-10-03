package mai.cn.film.domain;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import mai.cn.utils.Utils;

public class Graph {
	
	boolean withMine;
	static int MINCOINC=31; // TODO: parameter to finetune
	
	SortedMap<Integer,GraphNode> nodes; //all (me,movie,users)
	
	SortedMap<Integer,MovieUsers> movieNodes;
	SortedMap<Integer,UserMovies> userNodes;
	
	UserMovies me; //it's a UserMovies
	
	SortedMap<Integer, Film> films;
	
	Vector<Affinity> userAffinities, movieAffinities;
	
	public Graph(boolean withMine, SortedMap<Integer, Film> films ){
		this.withMine=withMine;
		nodes=new TreeMap<Integer,GraphNode>();
		movieNodes=new TreeMap<Integer,MovieUsers>();
		userNodes=new TreeMap<Integer,UserMovies>();
		this.films=films; // to construct the affinity lists linked to them
		me=null;
	}
	
	public int getIdForMovie(int idmovie){
		MovieUsers mu=movieNodes.get(idmovie);
		int idx;
		if (mu==null) {
			System.err.println("Movie Id:"+idmovie+" not found.");
			idx=-1;
		} else idx=mu.getIdxNode();
		return idx;
	}
	
	public int getIdForUser(int iduser){
		UserMovies um=userNodes.get(iduser);
		if (um==null){
			System.err.println("error");
		}
		return um.getIdxNode();
	}
	
	protected void addNodes(SortedMap<Integer,? extends GraphNode> gns){
		int id;
		
		for (GraphNode gn:gns.values()){
			id=add(gn);
			gn.setIdxNode(id);
		}
		System.out.println("Added "+gns.size()+" nodes");
	}

	public int addMovieToUser(MovieUsers mu){
		int iduser;
		int idx;
		SortedMap<Integer, UserRate> urs=mu.getUserRates();
		for (UserRate ur:urs.values()){
			iduser=ur.getIduser();
			idx=getIdForUser(iduser); // this call does not make sense if the nodes have not been added
			ur.setIdxNode(idx);
		}
		return mu.numberOfRates();
	}

	public int addUserToMovie(UserMovies um){
		int idmovie;
		int idx;
		SortedMap<Integer, MovieRate> mrs=um.getMovieRates();
		for (MovieRate mr:mrs.values()){
			idmovie=mr.getIdmovie();
			idx=getIdForMovie(idmovie);
			mr.setIdxNode(idx);
		}
		return um.numberOfRates();
	}
	
	public void addAllUserToMovie(SortedMap<Integer,UserMovies> users){
		int total=0;
		for (UserMovies user:users.values()){
			total+=addUserToMovie(user);
		}
		System.out.println("Added "+total+" user-movies.");
	}
	
	public void addAllMovieToUser(SortedMap<Integer,MovieUsers> movies){
		int total=0;
		for (MovieUsers movie:movies.values()){
			total+=addMovieToUser(movie);
		}
		System.out.println("Added "+total+" movie-users.");
	}
	
	protected void addMe() {
		userNodes.put(me.getId(),me);
	}
	
	public void addAll(UserMovies me, SortedMap<Integer,UserMovies> users, SortedMap<Integer,MovieUsers> movies){
		addMaps(me, users, movies);
		addNodes(userNodes);
		addNodes(movieNodes);
		addLinks();
	}

	public void addAllUsers(UserMovies me, SortedMap<Integer,UserMovies> users, SortedMap<Integer,MovieUsers> movies){
		addMaps(me, users, movies);
		addNodes(userNodes);			
		addLinks();
		userAffinities=generateEdges(userNodes, Type.USER, 0);	
		Affinity aff;
		Iterator<Affinity> it=userAffinities.iterator();
		int id1, id2, idold=-1;
		long end,ini=System.currentTimeMillis();
		UserMovies um1=null, um2;
		while (it.hasNext()){
			aff=(Affinity)it.next();
			id1=aff.getLabel1();
			if (id1!=idold){
				um1=userNodes.get(id1);
				idold=id1;
			}
			um1.addAffinity(aff);
			id2=aff.getLabel2();
			um2=userNodes.get(id2);
			um2.addAffinity(aff);
		}
		end=System.currentTimeMillis()-ini;
		System.out.println("Movie Affinities inserted in: "+end+" msec.");
	}
		
	public void addAllMovies(UserMovies me, SortedMap<Integer,UserMovies> users, SortedMap<Integer,MovieUsers> movies){
		addMaps(me, users, movies);
		addNodes(movieNodes);		
		addLinks();
		movieAffinities=generateEdges(movies, Type.MOVIE, MINCOINC);
		Affinity aff;
		Iterator<Affinity> it=movieAffinities.iterator();
		int id1, id2, idold=-1;
		Film film1=null, film2;
		long end,ini=System.currentTimeMillis();
		while (it.hasNext()){
			aff=(Affinity)it.next();
			id1=aff.getLabel1();		
			if (id1!=idold){
				film1=films.get(id1);
				idold=id1;
			}
			film1.addAffinity(aff);
			id2=aff.getLabel2();
			film2=films.get(id2);
			film2.addAffinity(aff);
		}
		end=System.currentTimeMillis()-ini;
		System.out.println("Movie Affinities inserted in: "+end+" msec.");
	}
	
	protected void addMaps(UserMovies me, 
			SortedMap<Integer, UserMovies> users,
			SortedMap<Integer, MovieUsers> movies) {
		this.me=me;
		setUserNodes(users);
		setMovieNodes(movies);
		if (withMine){
			addMe();
			addMyRates();
		}
	}
	
	public void addMyRates(){
	   //me and movieNodes
	   int id=me.getId();
	   int idmovie;
	   MovieUsers mu;
	   UserRate ur;
	   
	   SortedMap<Integer, MovieRate> mrs= me.getMovieRates();
	   for (MovieRate mr:mrs.values()){
		   idmovie=mr.getIdmovie();
		   mu=movieNodes.get(idmovie);
		   if (mu!=null){
			   ur=new UserRate(id,mr.getRate());
			   mu.addMovieRate(ur);
		   }
	   }
	}
	
	protected void setMovieNodes(SortedMap<Integer, MovieUsers> movies) {
		movieNodes=new TreeMap<Integer,MovieUsers>(movies);
	}


	protected void setUserNodes(SortedMap<Integer, UserMovies> users) {
		userNodes=new TreeMap<Integer,UserMovies>(users);
	}
	
	protected void addLinks( ) {
		if (withMine)
			addUserToMovie(me);
		addAllMovieToUser(movieNodes);
		addAllUserToMovie(userNodes);
	}
	
	private int add(GraphNode gn){
		int idx=nodes.size()+1; // base 1 for PAJEK
		nodes.put(idx,gn);
		return idx;
	}
	
	
	public double e2eLR(RateSet um1, RateSet um2, int mincoinc){
		double metric;
		Map<Integer, ? extends Rate> mr1,mr2;
		Collection<? extends Rate> col1;
		SimpleRegression sr;
		Rate ur2;
		int id;
		int rate1,rate2;
		int nCoinc=0;
		mr1=um1.getRates();
		mr2=um2.getRates();
		if (mr1.size()>mincoinc && mr2.size()>mincoinc){
			col1=um1.getRates().values();
			sr=new SimpleRegression();
			for (Rate ur:col1){
				id=ur.getId();
				ur2=mr2.get(id);
				if (ur2!=null){
					nCoinc++;
					rate1=ur.getRate();
					rate2=ur2.getRate();
					sr.addData(rate1, rate2);
				}
			}
			if (nCoinc>mincoinc){
				metric = sr.getR(); //correlation rate [-1,1]
			} else {
				metric=-2.0f; // no connection
			}
		} else {
			metric=-3.0f; // not enough user rates even to try
		}
				
		return metric;
	}
	
	public double e2e(RateSet um1, RateSet um2, int mincoinc){
		double metric;
		Map<Integer, ? extends Rate> mr1,mr2;
		Collection<? extends Rate> col1;
		Rate ur2;
		int id;
		int rate1,rate2;
		int nCoinc=0;
		mr1=um1.getRates();
		mr2=um2.getRates();
		
		if (mr1.size()>mincoinc && mr2.size()>mincoinc){
			col1=um1.getRates().values();
			Vector<Double> r1=new Vector<Double>();
			Vector<Double> r2=new Vector<Double>();
			double r1a[], r2a[];
			for (Rate ur:col1){
				id=ur.getId();
				ur2=mr2.get(id);
				if (ur2!=null){
					nCoinc++;
					rate1=ur.getRate();
					rate2=ur2.getRate();
					r1.add((double)rate1);
					r2.add((double)rate2);	
				}
			}
			if (nCoinc>mincoinc){
				PearsonsCorrelation pc=new PearsonsCorrelation();
				r1a=convertVectorToArray(r1);
				r2a=convertVectorToArray(r2);				
				metric = pc.correlation(r1a, r2a); //correlation rate [-1,1]
			} else {
				metric=-2.0f; // no connection
			}
		} else {
			metric=-3.0f; // not enough user rates even to try
		}
				
		return metric;
 	}
	
	
	private double[] convertVectorToArray(Vector<Double> v){
		double a[]=new double[v.size()];
		for (int i=0;i<a.length;i++){
			a[i]=v.get(i);
		}
		return a;
	}
	
	protected Vector<Affinity> generateEdges(SortedMap<Integer,? extends RateSet> rates,Type type, int mincoinc){		
		
		Vector<? extends RateSet> rateVect=new Vector<RateSet>(rates.values());
		int idxi,idxo;
		RateSet umi,umo;
		double metric; //correlation between two nodes
		int size;		
		Affinity aff;
		Vector<Affinity> vecAff=new Vector<Affinity>();
		
		// f(a,b) == f(b,a)!!!!
		// if not --- > change indexes
		idxo=0;
		size=rateVect.size();
		long endTime,iniTime=System.currentTimeMillis();
		while (idxo<size-1){
			idxi=idxo+1;
			umo=rateVect.get(idxo);			
			while (idxi<size){
				umi=rateVect.get(idxi);
				metric=e2e(umo,umi,mincoinc);				
				if (Double.compare(metric,-1.0d)>=0){
					aff=new Affinity(idxo+1,umo.getId(),idxi+1,umi.getId(),metric, type);	
					vecAff.add(aff);
				}
				idxi++;
			}
			idxo++;
			if (idxo % 25==0){
				endTime=System.currentTimeMillis();
				iniTime=endTime-iniTime;
				System.out.println(iniTime+" mseg. "+idxo+"/"+size+":Added "+vecAff.size()+" edges of type "+type);
				iniTime=endTime;
			}
		}
		System.out.println("Added "+vecAff.size()+" edges of type "+type);
		return vecAff;
	}

	/*******************************************************************************************************/
	/*******************************************************************************************************/
	/****************************************   PAJEK  *****************************************************/
	/*******************************************************************************************************/
	/*******************************************************************************************************/
	
	public void printPajekGraph(PrintStream ps){
		printPajekNodes(ps);
		printPajekUser2Movies(ps);
	}

	public void printPajekGraphUsers(PrintStream ps){
		printPajekNodes(ps);
		printPajekAffinities(ps, userAffinities);
	}
	
	public void printPajekGraphMovies(PrintStream ps){
		printPajekNodes(ps);
		printPajekAffinities(ps,movieAffinities);
	}
	
	public void generatePajekMovies(String filename){
		PrintStream ps=Utils.createFile(filename);
		printPajekGraphMovies(ps);
	}
	
	public void generatePajekUsers(String filename){
		PrintStream ps=Utils.createFile(filename);
		printPajekGraphUsers(ps);
	}
	
	public void generatePajek(String filename){
		PrintStream ps=Utils.createFile(filename);
		printPajekGraph(ps);
	}
	
	
	protected void printPajek(Affinity af, PrintStream ps){
		ps.println(af.getId1()+" "+af.getId2()+" "+af.getMetric());
	}
	
	
	protected void printPajekUser2Movies(PrintStream ps) {
		// edges
		ps.println("*Edges");// :1 \"movierates\"");
		// my rates are included in userNodes if required		
		// I take the ones in userNodes
		for (UserMovies um:userNodes.values()){			
			printPajek(um,ps);
		}
	}
	
	protected void printPajekAffinities(PrintStream ps, Vector<Affinity> affinities) {
		// edges
		ps.println("*Edges");// :1 \"movierates\"");
		
		// I take the ones in userNodes
		for (Affinity af:affinities){			
			printPajek(af,ps);
		}
	}

	protected void printPajekNodes(PrintStream ps) {
		int nNodes=nodes.size();
		// nodes
		ps.println("*Vertices "+nNodes);
		for (GraphNode gn:nodes.values()){
			ps.println(gn.getIdxNode()+" "+gn.getLabel()/*+" "+gn.getType()*/);
		}
	}
	
	public void printPajek(UserMovies um,PrintStream ps){
		SortedMap<Integer, MovieRate> mrs=um.getMovieRates();
		int idNodemovie,idNodeuser=um.getIdxNode();
		int rate;
		for (MovieRate mr:mrs.values()) {
			idNodemovie=mr.getIdxNode();
			rate=mr.getRate();
			ps.println(idNodeuser+" "+idNodemovie+" "+rate);
		}	
	}
	
}
