package Examples.SingleExample;// Compilation (CryptoLibTest contains the main-method):
//   javac CryptoLibTest.java
// Running:
//   java CryptoLibTest

import java.math.BigInteger;

public class CryptoLib {

	/**
	 * Returns an array "result" with the values "result[0] = gcd",
	 * "result[1] = s" and "result[2] = t" such that "gcd" is the greatest
	 * common divisor of "a" and "b", and "gcd = a * s + b * t".
	 **/
	public static int[] EEA(int a, int b) {
		// Note: as you can see in the test suite,
		// your function should work for any (positive) value of a and b.

        //Initialization of variables
		int s = 0;
		int t = 1;
        int r = b;
        int old_r = a;
        int old_s = 1;
        int old_t = 0;

        int[] result = new int[3];

        //As long as the remainder is not zero
        while(r!=0){
            //Find the divider
            int q = old_r/r;
            int temp = r;
            //Calculate new remainder
            r = old_r - q*temp;
            old_r = temp;
            //Storing Bezout coefficients
            temp = s;
            s = old_s - q*temp;
            old_s = temp;
            temp = t;
            t = old_t - q*temp;
            old_t = temp;
        }

		result[0] = old_r;
		result[1] = old_s;
		result[2] = old_t;
		return result;
	}
	/**
	 * Returns Euler's Totient for value "n".
	 **/
	public static int EulerPhi(int n) {
        if(n<0){
            return 0;
        }
        int result = 1;
        //Runs extended euclidian to check if relatively prime. Does this for every number between 2 and n.
        for (int i=2 ; i<n ; i++){
            if(EEA(i, n)[0] == 1){
                result++;
            }
        }
        return result;
	}

	/**
	 * Returns the value "v" such that "n*v = 1 (mod m)". Returns 0 if the
	 * modular inverse does not exist.
	 **/
	public static int ModInv(int n, int m) {
        int[] results;
        //transforms values since EEA does not handle negative values.
        if(n<0){
            results = EEA(m+n, m);
        } else {
            results = EEA(n,m);
        }
        //If the numbers are not relatively prime there is no inverse
        if(results[0]!=1){
            return 0;
        }
        //EEA returns the inverse
        int answer = results[1];
        //Changes the answer to positive.
        while (answer<0){
            answer=m+answer;
        }
        return answer;
	}

	/**
	 * Returns 0 if "n" is a Fermat Prime, otherwise it returns the lowest
	 * Fermat Witness. Tests values from 2 (inclusive) to "n/3" (exclusive).
	 **/
	public static int FermatPT(int n) {
        for(int i=2; i<(n/3) ; i++){
            //Unreadable code ahead this is the math it does: i^(n-1)%n != 1
            //Uses BigInteger to avoid overflow.
            if(!(new BigInteger(String.valueOf(i)).pow(n-1).mod(new BigInteger(String.valueOf(n)))
                    .equals(new BigInteger(String.valueOf(1))))){
                return i;
            }
        }
		return 0;
	}

	/**
	 * Returns the probability that calling a perfect hash function with
	 * "n_samples" (uniformly distributed) will give one collision (i.e. that
	 * two samples result in the same hash) -- where "size" is the number of
	 * different output values the hash function can produce.
	 **/
	public static double HashCP(double n_samples, double size) {
        //Recurse call to calculate 1 - π(n-1): 1/( 1-n/s )
        return 1-Birthday(n_samples-1, size);
	}
    private static double Birthday(double n, double s){
        if(n==0){
            return 1;
        } else {
            return (1 - n/s)*Birthday(n-1, s);
        }
    }

}
