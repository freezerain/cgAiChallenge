import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

class Player {

    public static void main(String args[]) throws IOException {
        Reader r = new Reader();
        Inventory inv = new Inventory();
        List<Action> actionList = new ArrayList<>();
        while (true) {
            long startTime = System.currentTimeMillis();
            actionList.clear();
            readAction(r, actionList, inv);
            System.err.println("InitTime: " + (System.currentTimeMillis() - startTime) + " ms");
            Action a = valueActionList(inv, actionList);
            if (a == null) System.out.println("REST Resting - null Action");
            else if (a.type == ActionType.BREW)
                System.out.println("BREW " + a.id + " Brewing id: " + a.id + " score: " + a.price);
            else if (a.type == ActionType.LEARN)
                System.out.println("LEARN " + a.id + " Learning id: " + a.id);
            else if (a.type == ActionType.CAST) {
                if (a.castable) {
                    if (a.repeatable) System.out.println(
                            "CAST " + a.id + " " + a.repeatN + " Casting id: " + a.id);
                    else System.out.println("CAST " + a.id + " Casting id: " + a.id);
                } else {
                    System.out.println("REST Resting to cast id: " + a.id);
                }
            } else System.out.println("REST Resting ActionType unknown");
            System.err.println("EndTime: " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    static void readAction(Reader r, List<Action> actionList, Inventory inv) throws IOException {
        int actionCount = r.nextInt();
        for (int i = 0; i < actionCount; i++){
            Action a = new Action(r.nextInt(), ActionType.valueOf(r.readString()),
                                  new int[]{r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt()},
                                  r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt() > 0,
                                  r.nextInt() > 0);
            if (a.type == ActionType.OPPONENT_CAST) continue;
            actionList.add(a);
        }
        for (int i = 0; i < 1; i++){//TODO less then 2 to get second player
            int totalCounter = 0;
            for (int j = 1; j <= 4; j++){
                inv.items[j] = r.nextInt();
                totalCounter += inv.items[j];
            }
            inv.items[0] = totalCounter;
            r.nextInt();
        }
        r.readLine();
    }


    static Action valueActionList(Inventory inv, List<Action> actionList) {
        double[] resValue = valueResources(inv, actionList);
        Action bestAction = null;
        int steps = 0;
        double actionValue = 0;
        for (Action action: actionList){
            //TODO ReadAHEAD TAX
          if (inv.isEnoughResources(action.type ==
                                      ActionType.LEARN ? new int[]{action.tomeIndex*-1, 0, 0, 0} :
                                              action.delta) &&
                (action.type != ActionType.CAST || inv.isEnoughSpace(action.delta))) {
                int newSteps = action.type == ActionType.BREW ? 1 :
                        (!action.castable) || action.type == ActionType.LEARN ? 3 : 2;
                double newValue = valueAction(action, resValue, inv);
                if(action.type == ActionType.LEARN) newValue+= action.taxCount*resValue[0];
                if (bestAction == null || actionValue / steps < newValue / newSteps) {
                    bestAction = action;
                    steps = newSteps;
                    actionValue = newValue;
                }
            }
        }
        System.err.println("ResValue: " + Arrays.toString(resValue));
        System.err.println(bestAction != null ? "BestAction " + bestAction : "Best action = null!");
        return bestAction;
    }


    static double valueAction(Action a, double[] resValue, Inventory inv) {
        if (a.type == ActionType.BREW) return a.price / 1.0;
        else {
            double value = 0.0;
            for (int i = 0; i < 4; i++){
                value += a.delta[i] * resValue[i];
            }
            if (a.repeatable) {
                int maxRepeat = 1;
                for (int i = 0; i < 4; i++){
                    if (a.delta[i] < 1 || resValue[i + 4] < 1) continue;
                    maxRepeat = Math.max(maxRepeat,
                                         ((int) resValue[i + 4]) + a.delta[i] - 1 / a.delta[i]);
                }
                int repeatN = Math.min(maxRepeat, inv.howManyRepeatable(a.delta));
                System.err.println("value Action: " + a);
                System.err.println("repeat min:" + maxRepeat + " howManyRepeat" + inv.howManyRepeatable(a.delta));
                value *= repeatN;
                a.repeatN = repeatN;
            }
            return value;
        }
    }

    static double[] valueResources(Inventory inv, List<Action> actionList) {
        // x4 value each, x4 amount each
        double[] resourceValue = new double[]{1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0};
        for (Action action: actionList){
            if (action == null || action.type != ActionType.BREW) continue;
            int[] missingItems = inv.getMissingItems(action.delta);
            double sum = Arrays.stream(missingItems).sum();
            //System.err.println("valRes Action id: " + action.id + " type: " + action.type);
            //System.err.println("valRes missingItems: " + Arrays.toString(missingItems));
            for (int j = 0; j < 4; j++){
                if (missingItems[j] > 0) {
                    //System.err.println("if resValue: " + resourceValue[j] +" resCount: "+ resourceValue[j + 4] +" price: "+ action.price + " sum: "+ sum +" missingCount: "+ missingItems[j]);
                    double pricePerRes = action.price / sum;
                    //System.err.println("if value*counter: " + resourceValue[j] * resourceValue[j + 4] + " price/sum*count: " + pricePerRes * missingItems[j]);
                    
                    if (resourceValue[j] < pricePerRes || resourceValue[j] * resourceValue[j + 4] < pricePerRes * missingItems[j]) {
                        resourceValue[j] = pricePerRes;
                        resourceValue[j + 4] = missingItems[j];
                    }
                }
            }
            //System.err.println("Changed resVal: " + Arrays.toString(resourceValue));
        }
        return resourceValue;
    }

    static class Inventory {
        int[] items = new int[5];

        public int howManyRepeatable(int[] delta) {
            //TODO Debug this here is a bug 
            int sum = 0;
            int divisor = Integer.MAX_VALUE;
            for (int i = 0; i < 4; i++){
                if (delta[i] < 0) divisor = Math.min(divisor, items[i + 1] / Math.abs(delta[i]));
                if (delta[i] != 0) sum += delta[i];
            }
            return sum == 0 ? divisor : Math.min(divisor, (10 - items[0]) / sum);
        }

        public boolean isEnoughSpace(int[] delta) {
            int itemsCount = Arrays.stream(delta)/*.filter(i -> i>0)*/.sum();
            return items[0] + itemsCount <= 10;
        }

        public int[] getMissingItems(int[] delta) {
            return IntStream.range(1, 5)
                    .map(i -> Math.abs(Math.min(delta[i - 1] + items[i], 0)))
                    .toArray();
        }

        boolean isEnoughResources(int[] delta) {
            for (int i = 1; i < 5; i++)
                if ((!(delta[i - 1] > 0)) && (!(items[i] + delta[i - 1] >= 0))) return false;
            return true;
        }
    }

    static class Action {
        int id;
        ActionType type;
        int[] delta;
        public int price;
        int tomeIndex;
        int taxCount;
        boolean castable;
        boolean repeatable;
        int repeatN;

        public Action(int id, ActionType type, int[] delta, int price, int tomeIndex,
                      int taxCount, boolean castable, boolean repeatable) {
            this.id = id;
            this.type = type;
            this.delta = delta;
            this.price = price;
            this.tomeIndex = tomeIndex;
            this.taxCount = taxCount;
            this.castable = castable;
            this.repeatable = repeatable;
        }

        @Override
        public String toString() {
            return "Action{" + "id=" + id + ", type=" + type + ", delta=" + Arrays.toString(delta) +
                   ", price=" + price + ", tomeIndex=" + tomeIndex + ", taxCount=" + taxCount +
                   ", castable=" + castable + ", repeatable=" + repeatable + ", repeatN=" +
                   repeatN + '}';
        }
    }

    enum ActionType {
        BREW, REST, CAST, OPPONENT_CAST, LEARN, WAIT, NONE
    }

    static class Reader {
        final private int BUFFER_SIZE = 1 << 16;
        private DataInputStream din;
        private byte[] buffer;
        private int bufferPointer, bytesRead;

        public Reader() {
            din = new DataInputStream(System.in);
            buffer = new byte[BUFFER_SIZE];
            bufferPointer = bytesRead = 0;
        }

        public Reader(String file_name) throws IOException {
            din = new DataInputStream(new FileInputStream(file_name));
            buffer = new byte[BUFFER_SIZE];
            bufferPointer = bytesRead = 0;
        }

        public String readString() throws IOException {
            byte[] buf = new byte[64]; // line length
            int cnt = 0, c;
            while ((c = read()) != -1) {
                if (c == '\n' || c == ' ') {
                    if (cnt != 0) break;
                    else continue;
                }
                buf[cnt++] = (byte) c;
            }
            return new String(buf, 0, cnt);
        }

        public String readLine() throws IOException {
            byte[] buf = new byte[64]; // line length
            int cnt = 0, c;
            while ((c = read()) != -1) {
                if (c == '\n') {
                    if (cnt != 0) break;
                    else continue;
                }
                buf[cnt++] = (byte) c;
            }
            return new String(buf, 0, cnt);
        }

        public int nextInt() throws IOException {
            int ret = 0;
            byte c = read();
            while (c <= ' ') c = read();
            boolean neg = (c == '-');
            if (neg) c = read();
            do ret = ret * 10 + c - '0'; while ((c = read()) >= '0' && c <= '9');
            if (neg) return -ret;
            return ret;
        }

        private void fillBuffer() throws IOException {
            bytesRead = din.read(buffer, bufferPointer = 0, BUFFER_SIZE);
            if (bytesRead == -1) buffer[0] = -1;
        }

        private byte read() throws IOException {
            if (bufferPointer == bytesRead) fillBuffer();
            return buffer[bufferPointer++];
        }

        public void close() throws IOException {
            if (din == null) return;
            din.close();
        }
    }
}