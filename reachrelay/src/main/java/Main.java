import java.io.IOException;

/**
 * Created by dexter on 08/08/15.
 */
public class Main {

    public static void main(String[] args) throws IOException {

        final int[] initialArray = new int[]{2, 1, 4, 5, 3, 4, 7, 9, 4, 8};
        final int totalSize = initialArray.length;

        int immediateGreatest = initialArray[totalSize - 1];
        System.out.println(immediateGreatest);

        for (int index = totalSize - 2; index >= 0; index--) {

            final int currentValue = initialArray[index];
            final int nextValue = initialArray[index + 1];

            if (currentValue >= nextValue) {

                if (currentValue < immediateGreatest)
                    System.out.println(immediateGreatest);
                else
                    System.out.println(immediateGreatest = currentValue);

            } else if (currentValue < nextValue)
                System.out.println(immediateGreatest = nextValue);
        }

    }
}
