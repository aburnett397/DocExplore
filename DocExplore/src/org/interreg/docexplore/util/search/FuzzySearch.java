/**
Copyright LITIS/EDA 2014
contact@docexplore.eu

This software is a computer program whose purpose is to manage and display interactive digital books.

This software is governed by the CeCILL license under French law and abiding by the rules of distribution of free software.  You can  use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".

As a counterpart to the access to the source code and  rights to copy, modify and redistribute granted by the license, users are provided only with a limited warranty  and the software's author,  the holder of the economic rights,  and the successive licensors  have only  limited liability.

In this respect, the user's attention is drawn to the risks associated with loading,  using,  modifying and/or developing or reproducing the software by the user in light of its specific status of free software, that may mean  that it is complicated to manipulate,  and  that  also therefore means  that it is reserved for developers  and  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards their requirements in conditions enabling the security of their systems and/or data to be ensured and,  more generally, to use and operate it in the same conditions as regards security.

The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you accept its terms.
 */
package org.interreg.docexplore.util.search;
import java.util.LinkedList;
import java.util.List;


public class FuzzySearch
{
	/**
	 * Performs a search for the occurrences of term in text.
	 * @param term
	 * @param text
	 * @param dist Allowed distance between term and occurrences.
	 * @return A list of occurrences. For each occurrence : {index in text, length, score (number of matching characters)}
	 */
	public static List<int []> process(String term, String text, int dist)
	{
		List<int []> res = new LinkedList<int []>();
		
		int [] scoreWindow = new int [dist+1];
		for (int i=0;i<scoreWindow.length;i++)
			scoreWindow[i] = 0;
		int cursor = 0;
		int noMatch = 0;
		
		for (int i = 1-term.length();i<text.length();i++)
		{
			scoreWindow[cursor] = 0;
			for (int j=0;j<term.length();j++)
				if (i+j>=0 && i+j<text.length() && Character.toLowerCase(term.charAt(j))==Character.toLowerCase(text.charAt(i+j)))
					scoreWindow[cursor]++;
			
			if (noMatch > 0)
				noMatch--;
			else if (scoreWindow[cursor] >= term.length()-dist)
			{
				res.add(new int [] {i, term.length(), scoreWindow[cursor]});
				//noMatch = dist-1;
			}
			else
			{
				int sum = scoreWindow[cursor];
				for (int j=0;j<dist;j++)
				{
					int prev = scoreWindow[(cursor+scoreWindow.length-(j+1))%scoreWindow.length];
					if (prev >= term.length()-dist)
						break;
					sum += scoreWindow[(cursor+scoreWindow.length-(j+1))%scoreWindow.length];
					if (sum >= term.length()-dist)
					{
						res.add(new int [] {i-(j+1), term.length()+j+1, sum-(j+1)});
						//noMatch = dist-1;
						break;
					}
				}
			}
			
			cursor = (cursor+1)%scoreWindow.length;
		}
		
		for (int [] match : res)
		{
			if (match[0] < 0) {match[1] += match[0]; match[0] = 0;}
			if (match[0]+match[1] >= text.length()) {match[1] = text.length()-match[0];}
		}
		
		return res;
	}
	
	public static double getScore(String term, String text, int dist)
	{
		if (term.length() == 0)
			return 1;
		List<int []> matches = process(term, text, dist);
		double score = 0;
		for (int [] match : matches)
		{
			double val = match[2]*1./term.length();
			if (val > score)
				score = val;
		}
		//System.out.println(term+" -> "+text+" ("+dist+") : "+score);
		return score;
	}
	
	public static void main(String [] args)
	{
//		String text = "Another thing that we see happening now is that people are starting to ask for win111ows binaries. " +
//			"Although we intend to provide binaries for major operating systems, we haven't really got round to it yet for Windows in particular.";
//		String term = "windows";
		
		String text = "coolos";
		String term = "coolos";
		
		System.out.println(getScore(term, text, 3));
		
//		List<int []> matches = process(term, text, 2);
//		for (int [] match : matches)
//			System.out.println(match[0]+" - "+(match[0]+match[1])+" ("+(match[2]*100/term.length())+"%) : "+text.substring(match[0], match[0]+match[1]));
	}
}
