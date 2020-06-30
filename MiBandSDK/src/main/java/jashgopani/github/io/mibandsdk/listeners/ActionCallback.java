package jashgopani.github.io.mibandsdk.listeners;

public interface ActionCallback {
    /**
     * Called on successful completion

     * @param data Fetched data
     */
    public void onSuccess(byte[] data);

    /**
     * Called on fail

     * @param errorCode Error code
     * *
     * @param msg       Error message
     */
    public void onFail(int errorCode,String msg);
}
