package iitb.CRF;

/*
 * If this is given as an argument to train() then during the training
 * loop this function will be called at each iteration. If evaluate
 * returns false, the training loop will exit.
 *
 * @author Sunita Sarawagi
 *
 */
public interface Evaluator {
    public boolean evaluate();
};
