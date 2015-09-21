create function f_z_test_by_pval(alpha float, x_bar float, test_val float, sigma float, n float)
--An independent one-sample z-test is used to test whether the average 
--of a sample differ significantly from a population mean. 
--INPUTS:
-- alpha = sigificant level which we will accept or reject at
-- x_bar = sample mean
-- test_val = specified value to be test
-- sigma = population standard deviation
-- n = size of the sample 

 RETURNS varchar
 --OUTPUT: Returns a simple "Is Significant" or "May have occured by random chance". 
 --In a real analysis, would probably use the Z-scores but this makes
 --for a clean and easiy interpretable demo. 
STABLE 
AS $$

 import scipy.stats as st
 import math as math

 #Z scores are measures of standard deviation. 
 #For example, if this calculation returns 1.96,it is interpreted 
 #as 1.96 standard deviations away from the mean
 z = (x_bar - test_val) / (sigma / math.sqrt(n)) 

 #The p-value is the probability that you have falsely rejected the null hypothesis
 p = st.norm.cdf(z)

 #in a Z test, the null hypothesis is that this distribution occured due to chance. 
 #When the p-value is very small, it means it is very unlikely (small probability) 
 #that the observed spatial pattern is the result of random processes, 
 # so you can reject the null hypothesis. 
 if p <= alpha:
   #rejecting the null hypothesis
   return 'Statistically Significant'
 else:
   return 'May Have Occurred By Random Chance'
 
$$LANGUAGE plpythonu;
