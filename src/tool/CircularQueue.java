package tool;

import java.util.ArrayList;
import java.util.Iterator;

public class CircularQueue<T> {
	private ArrayList<T> mQueue;
	private int mCursor = -1;
	
	public CircularQueue(int size) {
		mQueue = new ArrayList<T>(size);
	}
	
	public CircularQueue() {
		mQueue = new ArrayList<T>();
	}
	
	public ArrayList<T> getQueue() {
		return mQueue;
	}
	
	public Iterator<T> getIterator() {
		return mQueue.iterator();
	}
	
	public int size() {
		return mQueue.size();
	}
	
	public void clear() {
		mQueue.clear();
		mCursor = -1;
	}
	
	public boolean contains(T obj) {
		return mQueue.contains(obj);
	}
	
	public int indexOf(T obj) {
		return mQueue.indexOf(obj);
	}
	
	public boolean isEmpty() {
		return mQueue.isEmpty();
	}
	
	
	/**
	 * Add an item into circular queue <b>without moving cursor</b>.
	 * @param obj an item to be added into circular queue
	 * @param duplicable true if the array allows duplicate items
	 * @return true if adding object is complete
	 */
	public boolean enqueue(T obj, boolean duplicable) {
		if (mQueue.isEmpty()) mCursor = 0;
		
		if (duplicable == true) {
			mQueue.add(obj);
			return true;
		} else {
			if (mQueue.contains(obj)) {
				return false;
			} else {
				mQueue.add(obj);
				return true;
			}
		}
	}
	
	public boolean enqueue(T obj) {
		return enqueue(obj, true);
	}
	
	
	/**
	 * Add items into circular queue.
	 * @param list item list to be added into circular queue
	 * @param duplicable true if the array allows duplicate items
	 */
	public void enqueueAll(ArrayList<T> list, boolean duplicable) {
		for (T obj : list) {
			enqueue(obj, duplicable);
		}
	}
	
	public void enqueueAll(ArrayList<T> list) {
		mQueue.addAll(list);
	}
	
	
	/**
	 * Remove item of a given index.
	 * If cursor points the position of item to be removed, the cursor does not move and then points the next item.
	 * @param index index of item to be removed
	 * @return removed object
	 */
	public T remove(int index) {
		if (index >= mQueue.size())
			return null;
		T object = mQueue.remove(index);
		if (index < mCursor)
			mCursor -= 1;
		else
			mCursor %= mQueue.size();
		return object;
	}
	
	
	/**
	 * Remove a given item.
	 * @param obj item to be removed
	 * @param removeDuplicates if true, remove all elements corresponding to the given object in the queue
	 * @return true if removing the item is complete
	 */
	public boolean dequeue(T obj, boolean removeDuplicates) {
		if (removeDuplicates == false) {
			int index = mQueue.indexOf(obj);
			if (index == -1) {
				return false;
			} else {
				mQueue.remove(obj);
				if (index < mCursor)
					mCursor -= 1;
				else
					mCursor %= mQueue.size();
				return true;
			}
		} else {
			ArrayList<Integer> indices = new ArrayList<Integer>();
			int cntBeforeCursor = 0;
			for (int i = 0; i < mQueue.size(); i++) {
				if (obj == mQueue.get(i)) {
					indices.add(i);
					if (i < mCursor) cntBeforeCursor++;
				}
			}
			
			if (indices.isEmpty()) {
				return false;
			} else {
				for (int i = indices.size() - 1; i >= 0; i--)
					mQueue.remove(indices.get(i));
				mCursor -= cntBeforeCursor;
				mCursor %= mQueue.size();
				return true;
			}
		}
	}
	
	public boolean dequeue(T obj) {
		return dequeue(obj, false);
	}
	
	public boolean dequeue() {
		if (mQueue.isEmpty()) {
			return false;
		} else {
			remove(mCursor);
			return true;
		}
	}
	
	
	/**
	 * Get item of the given position(index) with <b>moving cursor</b>.
	 * @param index index of circular queue
	 * @return object of the given index in the circular queue
	 */
	public T get(int index) {
		if (mQueue.isEmpty() || index < 0 || index >= mQueue.size())
			return null;
		mCursor = index;
		return mQueue.get(mCursor);
	}
	
	
	/**
	 * Get item of current cursor position <b>without moving cursor</b>.
	 * @return current item
	 */
	public T getCurrentItem() {
		if (mQueue.isEmpty())
			return null;
		return mQueue.get(mCursor);
	}
	
	
	/**
	 * Get item of the position with <b>moving cursor</b> as much as 'degree'.
	 * @param degree moving degree
	 * @return item of circular queue after moving cursor
	 */
	public T getNextItem(int degree) {
		if (mQueue.isEmpty())
			return null;
		mCursor = (mCursor + degree) % mQueue.size();
		return mQueue.get(mCursor);
	}
	
	public T getNextItem() {
		return getNextItem(1);
	}
	
	
	/**
	 * Get item of the given index <b>without moving cursor</b>.
	 * @param index queue index of item to be get
	 * @return item of the a index
	 */
	public T peek(int index) {
		if (mQueue.isEmpty() || index < 0 || index >= mQueue.size())
			return null;
		return mQueue.get(index);
	}
	
	
	/**
	 * Get the next item as much as 'degree' <b>without moving cursor</b>.
	 * @param degree moving degree
	 * @return item away from current position as much as 'degree'
	 */
	public T peekNext(int degree) {
		if (mQueue.isEmpty())
			return null;
		return mQueue.get((mCursor + degree) % mQueue.size());
	}
	
	public T peekNext() {
		return peekNext(1);
	}
	
	
	/**
	 * Set cursor with a given index.
	 * @param index
	 */
	public void setCursor(int index) {
		if (mQueue.isEmpty() || index < 0 || index >= mQueue.size())
			return;
		mCursor = index;
	}
	
	public void setCursor(T obj) {
		if (mQueue.isEmpty())
			return;
		mCursor = mQueue.indexOf(obj);
	}
	
	
	/**
	 * Move cursor to the next by one.
	 * @return cursor
	 */
	public int next() {
		if (mQueue.isEmpty())
			return -1;
		mCursor = (mCursor + 1) % mQueue.size();
		return mCursor;
	}
	
	
	/**
	 * Get current cursor position
	 * @return current cursor position
	 */
	public int getCursor() {
		return mCursor;
	}
	
	public int getCursor(T obj) {
		return mQueue.indexOf(obj);
	}
	
	public int peekNextCursor() {
		if (mQueue.isEmpty())
			return -1;
		return (mCursor + 1) % mQueue.size();
	}
}