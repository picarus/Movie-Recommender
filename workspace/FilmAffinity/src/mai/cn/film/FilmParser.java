package mai.cn.film;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mai.cn.film.db.FilmDB;
import mai.cn.film.domain.Film;
import mai.cn.film.domain.MyRatedFilm;
import mai.cn.film.domain.UserRecommendedFilm;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

public class FilmParser {

	int nFilms=0;
	int nUsers=0;
	int nMyRates=0;
	int nAvgRates=0;
	int nUserRates=0;
	int nUserRecRates=0;
	int nErrors=0;
	FilmDB filmDB;
	
	//static String pathBase="D:\\weblech\\soulmates\\www.filmaffinity.com\\en\\";
	static String pathTemplate="D:\\weblech\\templates\\";
	static String pathMyRatings="D:\\weblech\\soulmates\\www.filmaffinity.com\\en\\sharerating\\876396\\";
	static String pathUserRatings="D:\\weblech\\soulmates\\www.filmaffinity.com\\en\\user ratings\\";
	static String pathUserRecommend="D:\\weblech\\soulmates\\www.filmaffinity.com\\en\\user recom\\";
	static String pathFilms="D:\\weblech\\soulmates\\www.filmaffinity.com\\en\\";
	static String pathBase=pathTemplate;	
	
	FilmParser(FilmDB filmDB){
		this.filmDB=filmDB;
	}
	
	
	String readFile(String path) throws  IOException {
		return readFile(path,StandardCharsets.ISO_8859_1);
	}
		
	String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}
	
	public void readTemplates(File[] listOfFiles){
   	 	
   	 	Document doc;
   	 	File file;
   	 	String fileName;
   	 	int end=listOfFiles.length;
   	 	int size;
   	 	List<UserRecommendedFilm> lurf;
   	 	List<Film> lf=new ArrayList<Film>();
   	 	Film film;
   	 	List<MyRatedFilm> lmrf=new ArrayList<MyRatedFilm>();
   	 	MyRatedFilm mrf;
        for (int i = 0; i < end; i++) {
        	file=listOfFiles[i];        	
			if (file.isFile()) {
				try {
					doc = Jsoup.parse(file, "ISO_8859_1");
					fileName=file.getName();
					if (fileName.startsWith("film")){
						film=parseFilm(doc);
						lf.add(film);
						nFilms++;
						if (nFilms % 5000==0){
							filmDB.batchInsertFilm(lf);
							filmDB.batchInsertFilmTitle(lf);
							lf.clear();
						}
					} else if (fileName.startsWith("user ")||fileName.startsWith("userratings")){
						// user recom movies and rating
						lurf=parseUserRecommendedFilm(doc);
						size=lurf.size();
						if (fileName.startsWith("user ")){
							nUserRecRates+=size;
							nUsers++;
							filmDB.batchInsertURF(lurf, "matesrec");
						} else {
							nUserRates+=size;
							filmDB.batchInsertURF(lurf,"matesmovies");
						}
						
					} else if (fileName.matches("[0-9].*")) { 
						// my ratings
						mrf=parseMyRatedFilm(doc);
						lmrf.add(mrf);
						nMyRates++;
						if (nMyRates % 5000==0){
							filmDB.batchInsertMyRatedFilm(lmrf);
							lmrf.clear();
						}					
					} else {
//						if (fileName.startsWith("FilmAff")){					
//						// my list of ratings
//						// redundant  --> not required so far
//						} else if (fileName.startsWith("globalrec")){ 
//						// globalrec
//						} else if (fileName.contains("cast")){
//						// actor movies						
//						} else if (fileName.contains("director")) {
//						// director movies																			
//						} else if (fileName.startsWith("moviegenre")){
//						// low prio
//						} else if (fileName.startsWith("awards")){
//						// awards
//						}
					}
				}
				catch(IOException e){
					
				}
		    }
		}
        if (!lf.isEmpty()){
	    	filmDB.batchInsertFilm(lf);
	    	filmDB.batchInsertFilmTitle(lf);
        }
        if (!lmrf.isEmpty()){
        	filmDB.batchInsertMyRatedFilm(lmrf);
        }
						
	}
	
	private Film parseFilm(Document doc) {
		Film film;
		int filmId,rateNumbers;
		String filmTitle;
		float avgrate;
		int end;
		//filmID
		Element id=doc.select("div.rate-movie-box").first();
		filmId=Integer.parseInt(id.attr("data-movie-id"));
		//filmTitle
		Element title=doc.select("title").first(); //todo: remove - FilmAffinity
		TextNode n=(TextNode)title.childNode(0);
		filmTitle=n.getWholeText();
		end=filmTitle.lastIndexOf("(");
		filmTitle=filmTitle.substring(0, end-1);
		//avgrate			
		Element rate=doc.select("[style=color:#990000; font-size:22px; font-weight: bold;]").first();
		if (rate==null) { // film not rated
			avgrate=-1.0f;
			rateNumbers=0;
		} else {
			n=(TextNode)rate.childNode(0);
			avgrate=Float.parseFloat(n.getWholeText());
			//rateNumbers	
			Element numbers=doc.select("td[align=center]:containsOwn( votes\\))").first();
			n=(TextNode)numbers.childNode(0);
			String strNumber=n.getWholeText();
			end=strNumber.length()-" votes)".length();
			strNumber=strNumber.substring(1,end);
			try{
				rateNumbers=parseNumberOfVotes(strNumber);
			} catch (NumberFormatException e){
				System.err.println("Number Format Exception:"+filmId);
				rateNumbers=0;
				nErrors++;
			}
		}
		
		film=new Film(filmId,avgrate,rateNumbers,filmTitle);
		System.out.println("Id:"+filmId+" Title:"+filmTitle+" AvgRate("+rateNumbers+"):"+avgrate);		
		return film;
	}

	private List<UserRecommendedFilm> parseUserRecommendedFilm(Document doc) {
		
		
		Element userId=doc.select("input[id=friend-id]").first();
		int user=Integer.parseInt(userId.attr("value"));
		
		List<UserRecommendedFilm> lURM=new ArrayList<UserRecommendedFilm>();
		UserRecommendedFilm urm;
		Elements titles=doc.select("a.ntext");
		Elements rates=doc.select("span.wrat[style=font-size:17]");
		String strTitle;
		int userRate;
		int filmId;
		TextNode title;
		Node node;
		TextNode rate;
		String href;
		Iterator<Element> itElms=titles.iterator();
		Iterator<Element> itRates=rates.iterator();
		
		while (itElms.hasNext() && itRates.hasNext()){
			node=itElms.next();	
			href=node.attr("href");
			filmId=getFilmIdFromHRef(href);
			title=(TextNode) node.childNode(0);
			strTitle=title.getWholeText();
			rate=(TextNode)itRates.next().childNode(0);
			userRate=Integer.parseInt(rate.getWholeText().trim());
			System.out.println("Id:"+filmId+" Title:"+strTitle+" Rate:"+userRate);
			urm=new UserRecommendedFilm(filmId, userRate, user, strTitle);
			lURM.add(urm);
		}
		
		if (itElms.hasNext()){
			System.err.println("More movies than rates");
		}
		
		if ( itRates.hasNext()){
			System.err.println("More rates than movies");
		}
		
		return lURM;
		
	}

	int getIdFromMyRates(Document doc) {
		String link=doc.select("div.mc-image a").first().attr("href");
		return getFilmIdFromHRef(link);
	}

	protected int getFilmIdFromHRef(String link) {
		int begin="/en/film".length();
		int end=link.indexOf(".");		
		return Integer.parseInt(link.substring(begin, end));
	}
	
	int getMyRateFromMyRates(Document doc){
		// it may be null --> movie not seen
		int rate;
		Element myrate = doc.select("span.rating-number").first();
		if (myrate.childNodeSize()==0)
			rate= -1;
		else {
			TextNode n=(TextNode)myrate.childNode(0);
			rate=Integer.parseInt(n.getWholeText());
		}
		return rate;
	}

	
	int getRateNumberFromMyRates(Document doc){
		int number;
		Element avgrate = doc.select("div.mc-rating").first();
		if (avgrate.childNodeSize()==0)
			number= -1;
		else {
			TextNode n=(TextNode)avgrate.childNode(1);
			String nStr=n.getWholeText();
			number = parseNumberOfVotes(nStr);
		}
		return number;
	}

	protected int parseNumberOfVotes(String nStr) {
		int number;
		nStr=nStr.replace(",","").trim();			
		number=Integer.parseInt(nStr);
		return number;
	}

	
	 float getAvgRateFromMyRates(Document doc){
		float rate;
		Element avgrate = doc.select("div.mc-rating").first();
		if (avgrate.childNodeSize()==0)
			rate= -1.0f;
		else {
			TextNode n=(TextNode)avgrate.childNode(0).childNode(0);			
			rate=Float.parseFloat(n.getWholeText());
		}
		return rate;
	}
	
	 MyRatedFilm parseMyRatedFilm(Document doc){
		MyRatedFilm mRM;
		int filmId=getIdFromMyRates(doc);
		int myrate = getMyRateFromMyRates(doc);
		float avgrate = getAvgRateFromMyRates(doc);	
		int rateNumbers=getRateNumberFromMyRates(doc);
		System.out.println("Film Id:"+filmId+" My Rate:"+myrate+" Avg. Rate("+rateNumbers+"):"+avgrate);
		mRM=new MyRatedFilm(filmId,myrate,avgrate,rateNumbers,"");
		return mRM;
	}
	 
	public void print(){
		System.out.println("***********************************");
		System.out.println("N Errors Parsing:"+nErrors);	
		System.out.println("N Errors DB:"+filmDB.getnErrorsDB());
		System.out.println("N Films:"+nFilms);
		System.out.println("N My Rates:"+nMyRates);
		System.out.println("N Users:"+nUsers);
		System.out.println("N User Rates:"+nUserRates);
		System.out.println("N User Rec Rates:"+nUserRecRates);
		System.out.println("***********************************");
	}
	 
	
	public static void main(String[] args) {
		FilmDB filmDB=FilmDB.getInstance();
		FilmParser filmParser=new FilmParser(filmDB);
		File folder;
		folder = new File(pathFilms);
		filmParser.processFolder(folder);
//		filmParser.print();
//		folder = new File(pathMyRatings);
//		filmParser.processFolder(folder);
//		filmParser.print();
//		folder = new File(pathUserRecommend);
//		filmParser.processFolder(folder);
//		filmParser.print();
//		folder = new File(pathUserRatings); 
//		filmParser.processFolder(folder);
		filmParser.print();
		filmDB.close();
	}

	protected void processFolder(File folder) {
		File[] listOfFiles;
		listOfFiles = folder.listFiles();
		readTemplates(listOfFiles);
	}

}
