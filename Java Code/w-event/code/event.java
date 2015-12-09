import java.io.*;
import java.util.*;
import cern.jet.random.*;



//import java.nio.charset.Charset; 
//import com.csvreader.CsvReader;  
//import com.csvreader.CsvWriter;  
/**
 *
 * @author gkellaris
 */
public class event {

    final static double eps = 1.0; //epsilon value
    static int ts = 1320; //number of timestamps to read
    static int mids = 89997	; //number of columns
    static int max = 120; //maximum number of consecutive timestamps a user can be,������������еĲ��� w
    static double eps1; //budget for dissimilarity checking
    static double eps2; //budget for publishing

    public static void main(String[] args) 
	{
	        double[][] histogram = new double[mids][ts]; //raw data
	        double[][] new_histogram = new double[mids][ts]; //noisy data
	        
	        cern.jet.random.engine.RandomEngine generator;
	        generator = new cern.jet.random.engine.MersenneTwister(new java.util.Date());	//����α�����

	        System.out.print("Loading data... ");				
	        LoadData("h:\\data1320.csv", histogram);
	        System.out.println("Done! ");

	        System.out.println("Start Processing Algorithm BD ...");
//	        histogram = new double[mids][ts];
	        BD(new_histogram, histogram, generator);

//	        System.out.println("Start Processing Algorithm BC ...");
////	        histogram = new double[mids][ts];
//	        BC(new_histogram, histogram, generator);
//
//	        System.out.println("Start Processing Algorithm Sample ...");
////	        histogram = new double[mids][ts];
//	        Sample(new_histogram, histogram, generator);
    }

    //read data and store them in histogram
    //each row in the data file is a column
    //for each column the value of each timestamp is seperated by a ';'
    static void LoadData(String filename, double[][] histogram) {
        File trajFile = new File(filename);
        int counter = 0;
        try {
            if (trajFile.exists()) {
                BufferedReader inFile = new BufferedReader(new FileReader(trajFile));//FileReader:�����ַ�������������Ѵ��ڵ��ļ���������ļ������ڵĻ���������;BufferedReaderӵ��8192�ַ�������
				
                String line = inFile.readLine();
                while (line != null) {
                    java.util.StringTokenizer st = new java.util.StringTokenizer(line, ";");//StringTokenizer�����ָ�String��Ӧ����
                    st.nextToken();	//��һ���ֶ�
                    st.nextToken();
                    for (int i = 0; i < ts; i++) {
                        int c = Integer.parseInt(st.nextToken());//���������string�ַ���������ת��ΪInteger��������
                        histogram[counter][i] = (double) (c);
                    }
                    counter++;
                    line = inFile.readLine();//������
                }
                	        	
                inFile.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    static void BD(double[][] new_histogram, double[][] histogram, cern.jet.random.engine.RandomEngine generator) {
//        eps1 = ((double) max * eps) / (double) mids; //budget for dissimilarity checking
    	
        eps1 = eps / (2 * (double) max);  //budget for dissimilarity checking�����ֵ�ǹ̶���
        eps2 = eps - eps1; //budget for publishing
        double budget = eps2;
        ArrayList<Integer> when = new ArrayList(); //keep truck of the releases
        ArrayList<Double> bud = new ArrayList(); //keep track of the budget per release
        int number = 0;
        int k = 0; 
        double avg = 0;
        System.out.println("Debug:histogram [0][0]:" + histogram[0][0]);
        
        while (k != ts) {
            double cur = budget; // ��ǰ���õ���˽Ԥ�㣬���Ԥ����ÿ��kѭ��ʱ�ı䣬�ݼ���������ն�����
            budget = cur / (double) 2.0; //use half of the available budget,M2�Ŀ���Ԥ��Ϊ��Ԥ���һ��
            
//            if( 0 < k && k < 100){
//            	System.out.println("Current k :" + k);
//                System.out.println("Current budget :" + budget);
//            }
            
            //publish
            for (int i = 0; i < mids; i++) {	//�Ե�k�е�����Ԫ�ؼ���
                new_histogram[i][k] = histogram[i][k] + Distributions.nextLaplace(generator) * budget;
            }
            when.add(k);	//������k��������
            bud.add(budget);
            number++;
            budget = cur - budget; //remaining budget
            int j;
            for (j = k + 1; j < ts; j++) {	//����j��Ϊk�еĺ�һ�У������ѭ������k=0��ʼ���������е�ÿһ��
                avg = 0;
                //calculate dissimilarity
                for (int i = 0; i < mids; i++) {	//����j��Ԫ�غ�k��Ԫ�صĲ�����
                    avg += ((double) Math.abs(new_histogram[i][k] - histogram[i][j])) / (double) (mids);
                }
                //add noise to dis
                avg += Distributions.nextLaplace(generator) / (double) eps1;//+1/(last);��������dis���룿
                
          //********������M1�׶εļ������*************
                
                int flag = 0;
                
                //budget recycling
                if (number > 0) {
                    if (j - when.get(0) == max) {			//get����ȡָ��λ�õ�Ԫ�أ���0���ǻ�ȡ��һ��Ԫ�أ�������ζ�ŵ��ﴰ�ڴ�Сʱ��ʼ����Ԥ��
                        budget += bud.get(0);				//����Ԥ���м���Ԥ�������еĵ�һ��Ԥ��ֵ
                        when.remove(0);						//�Ƴ�ԭ���ڵĵ�һ������ֵ
                        bud.remove(0);						//�Ƴ�Ԥ�����е�һ��ֵ
                        number--;
                        flag = 1;
                    } else {
                        if (flag == 1) {
                            flag = 0;
                        }
                    }
                }
                
                //check if is better to publish or approximate
                double test = 0;
                test = budget / (double) 2.0;
                if (avg > (1.0 / (double) test)) {				//ƽ������Ƿ�����������������
                    k = j;						//���avg��������ֱ�ӷ����������ݴ��������򷢲�ֱ�Ӽ������ֵ                  
                    break;
                } else {						//���avg�����С�ڻ����ֱ�ӷ����������ݴ��������򷢲�����ֵ������������ΪNULL
                    for (int i = 0; i < mids; i++) {	//���ѭ����k���е�ÿһ��ֵ����j�У���j�������е�ֵ��ȡk���ϵ�ֵ
                        new_histogram[i][j] = new_histogram[i][k];		//��������ֵnew_histogram[i][k]
                    }
                }
            }// for (j = k + 1; j < ts; j++) ended
            
            if (j == ts) {
                k = ts;
            }
        }//while ended
	
//    	System.out.println(Arrays.deepToString(new_histogram)); //debug:���histogram��ά����

//�����ļ�
//        File file_output = new File("h:\\array.txt");  //����������ݵ��ļ�     
//        try{
//        	FileWriter out_file = new FileWriter(file_output);  //�ļ�д���� 
//        	
//            for(int i = 0;i < mids;i++)
//            {
//            	for(int j = 0;j < ts;j++)
//            	{
//            		out_file.write(new_histogram[i][j]+",");
//            	}
//        
//            	out_file.write("\r\n");
//            }          
//        	out_file.close();
//        }
//        catch(IOException e){
//        	System.err.println(e);
//        }
//�����ļ�����
//******************//
        //System.out.println("new_istogram [0][0]:" + new_histogram[0][0]);
//����������;������
        int p = 0;
        int q = 0;
        double sum = 0;
        double mae = 0;
        int count = 0;
        double percent = 0;
        for (p = 0; p < mids; p++) {
        	for (q = 0; q < ts; q++) {
        		
//        		if(histogram[p][q] != 0){       			
//            		sum += (double) Math.abs(new_histogram[p][q] - histogram[p][q]);
//            		count++;       			       			
//        		}
        		sum += (double) Math.abs(new_histogram[p][q] - histogram[p][q]);
//        		count++; 
        		if(histogram[p][q] == 0){
        			count++;
        		}
        	}        	
        }
        mae = sum / (ts*mids);
//        mae = sum / count;
        percent = (double)count/(ts*mids);
        
        System.out.println("BD_MAE : " + mae);
        System.out.println("Zero : " + count + "   Percentage: " + percent);
        
        
        int z = 0;
        int y = 0;
        double mre_sum = 0;
        double sum_1 = 0;
        double mre = 0;
       
        for (z = 0; z < mids; z++) {
        	for (y = 0; y < ts; y++) {
        		
//        		if(histogram[z][y] != 0){      		    
//        			mre_sum += ((double) Math.abs(new_histogram[z][y] - histogram[z][y]))/histogram[z][y];
//        		   	
//        		}else{
//        			mre_sum += ((double) Math.abs(new_histogram[z][y] - histogram[z][y]))/1;
//        		}
        		
        		if(histogram[z][y] != 0){      		    
        			mre_sum += ((double) Math.abs(new_histogram[z][y] - histogram[z][y]))/histogram[z][y];        		   	
        			sum_1++;
        		}
        		
        		
        	}        	
        }
//        mre = mre_sum / (ts*mids);
        mre = mre_sum / sum_1;     
        System.out.println("BD_MRE : " + mre);        
        
        
    }
//************** BD�㷨��������***************

//************** BC�㷨������ʼ***************
    
static void BC(double[][] new_histogram, double[][] histogram, cern.jet.random.engine.RandomEngine generator) {
        eps1 = ((double) max * eps) / (double) mids; //budget for dissimilarity checking
        eps2 = eps - eps1; //budget for publishing
        double budget = eps2 / (double) max; //set assigned budget at each timestamp
        ArrayList<Integer> when = new ArrayList(); //keep track of releases
        int k = 0;
        double avg = 0;
        int phase = 0; 
        while (k != ts) {
            //publish
            for (int i = 0; i < mids; i++) {
                new_histogram[i][k] = histogram[i][k] + Distributions.nextLaplace(generator) * budget;
            }
            when.add(k);
            phase = 1;
            int j;
            for (j = k + 1; j < ts; j++) {
                avg = 0;
                //calculate dissimilarity
                for (int i = 0; i < mids; i++) {
                    avg += ((double) Math.abs(new_histogram[i][k] - histogram[i][j])) / (double) (mids);
                }
                avg += Distributions.nextLaplace(generator) / (double) eps1;
                //find out if we skipped or published or nullified budgets
                if (phase == 1) {
                    budget -= eps2 / (double) max;
                }
                if (budget < eps2 / (double) max) {
                    phase = 0;
                }
                if (phase == 0) {
                    budget += eps2 / (double) max;
                }
                
                //publish if it is better than approximating
                if ((avg > (1.0 / (double) budget) && phase == 0) || budget >= eps2) {
                    k = j;
                    break;
                } else {
                    for (int i = 0; i < mids; i++) {
                        new_histogram[i][j] = new_histogram[i][k];
                    }
                }
            }
            
            if (j == ts) {
                k = ts;
            }
        }
        
  //����������;������
        int p = 0;
        int q = 0;
        double sum = 0;
        double mae = 0;
//        int count = 0;
        for (p = 0; p < mids; p++) {
        	for (q = 0; q < ts; q++) {
        		
//        		if(histogram[p][q] != 0){       			
//            		sum += (double) Math.abs(new_histogram[p][q] - histogram[p][q]);
//            		count++;       			       			
//        		}
        		sum += (double) Math.abs(new_histogram[p][q] - histogram[p][q]);
//        		count++;  
        	}        	
        }
        mae = sum / (ts*mids);
        System.out.println("BC_MAE : " + mae);
        
        int z = 0;
        int y = 0;
        double mre_sum = 0;
        double sum_1 = 0;
        double mre = 0;
       
        for (z = 0; z < mids; z++) {
        	for (y = 0; y < ts; y++) {
        		
        		if(histogram[z][y] != 0){      		    
        			mre_sum += ((double) Math.abs(new_histogram[z][y] - histogram[z][y]))/histogram[z][y];
        		   	
        		}else{
        			mre_sum += ((double) Math.abs(new_histogram[z][y] - histogram[z][y]))/1;
        		}
//        		sum_1 += histogram[z][y];
        	}        	
        }
        mre = mre_sum / (ts*mids);
        System.out.println("BC_MRE : " + mre);        
            
        
    }

    static void Sample(double[][] new_histogram, double[][] histogram, cern.jet.random.engine.RandomEngine generator) {
        int k = 0;
        while (k != ts) {
            //publish
            for (int i = 0; i < mids; i++) {
                new_histogram[i][k] = histogram[i][k] + Distributions.nextLaplace(generator) / eps;
            }
            int j;
            
            //skip max timestamps
            for (j = k + 1; j < ts; j++) {
                if ((j - k) == max) {
                    k = j;
                    break;
                } else {
                    for (int i = 0; i < mids; i++) {
                        new_histogram[i][j] = new_histogram[i][k];
                    }
                }
            }
            if (j == ts) {
                k = ts;
            }
        }
        
//����������;������
        int p = 0;
        int q = 0;
        double sum = 0;
        double mae = 0;
//      int count = 0;
        for (p = 0; p < mids; p++) {
        	for (q = 0; q < ts; q++) {
        		
//        		if(histogram[p][q] != 0){       			
//            		sum += (double) Math.abs(new_histogram[p][q] - histogram[p][q]);
//            		count++;       			       			
//        		}
        		sum += (double) Math.abs(new_histogram[p][q] - histogram[p][q]);
//        		count++;  
        	}        	
        }
        mae = sum / (ts*mids);
        System.out.println("Sample_MAE : " + mae);
        
        int z = 0;
        int y = 0;
        double mre_sum = 0;
        double sum_1 = 0;
        double mre = 0;
       
        for (z = 0; z < mids; z++) {
        	for (y = 0; y < ts; y++) {
        		
        		if(histogram[z][y] != 0){      		    
        			mre_sum += ((double) Math.abs(new_histogram[z][y] - histogram[z][y]))/histogram[z][y];
        		   	
        		}else{
        			mre_sum += ((double) Math.abs(new_histogram[z][y] - histogram[z][y]))/1;
        		}
//        		sum_1 += histogram[z][y];
        	}        	
        }
        mre = mre_sum / (ts*mids);
        System.out.println("Sample_MRE : " + mre);  
    }

}  	//�ܺ�������
