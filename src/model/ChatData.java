package model;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by jordan on 2/7/18.
 */

// some code was used from http://www.baeldung.com/java-blocking-queue and cs601
public class ChatData {
    private LinkedList chatsQueue = new LinkedList();
    private int capacity;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock writeLock = lock.writeLock();
    private Lock readLock = lock.readLock();

    public  ChatData (){
        this.capacity = 100;
    }

    public void addMessage( DataServerMessages.Chat message){
        try{
            writeLock.lock();
            chatsQueue.add(message);
        }finally{
            writeLock.unlock();
        }
    }


    public synchronized  void showMessage(){
        if(chatsQueue.size()>0){
            try{
                readLock.lock();
                Iterator<DataServerMessages.Chat> iter = chatsQueue.iterator();
                while(iter.hasNext()){
                    DataServerMessages.Chat message = iter.next();
                    String sender = message.getFrom();
                    String mess = message.getMessage();

                    System.out.println(sender+" said: "+ mess);
                }
            }finally {
                readLock.unlock();
            }

        }else{
            System.out.println("nothing cached");
        }

    }
    public synchronized List<DataServerMessages.Chat> returnMessageList(){
        return chatsQueue;
    }

    public synchronized void clearHistory(){
        chatsQueue.clear();
    }
    public synchronized void replaceHistory(List historyList){
        chatsQueue.clear();
        for(int i =0; i<historyList.size(); i++){
            chatsQueue.add(historyList.get(i));
        }
    }

}





