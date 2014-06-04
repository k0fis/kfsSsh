package kfs.kfsUtils.kfsSsh;

import java.util.*;

/**
 *
 * @author pavedrim
 * @param <T>
 */
public class SortingList<T extends Object> implements List<T> {

    private ArrayList<T> lst;
    private Comparator<T> cmp;

    public SortingList(Comparator<T> cmp) {
        this.cmp = cmp;
        lst = new ArrayList<T>();
    }

    public SortingList(Comparator<T> cmp, Iterable<? extends T> iter) {
        this(cmp);
        add(iter);
    }

    @Override
    public boolean add(T item) {
        boolean ret = lst.add(item);
        Collections.sort(lst, cmp);
        return ret;
    }
    public final boolean add(Iterable<? extends T> iter) {
        boolean ret = _add(iter);
        if (ret) {
            Collections.sort(lst, cmp);
        }
        return ret;
    }

    private boolean _add(Iterable<? extends T> col) {
        boolean ret = true;
        for (T t: col) {
            if (ret) {
                ret = ret && lst.add(t);
            } else {
                break;
            }
        }
        Collections.sort(lst, cmp);
        return ret;
    }

    @Override
    public int size() {
        return lst.size();
    }

    @Override
    public boolean isEmpty() {
        return lst.isEmpty();
    }

    public boolean containsB(T o) {
        for (T q : lst) {
            if (cmp.compare(o, q) == 0) {
                return true;
            }
        }
        return false;
        //return lst.contains(o);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean contains(Object o) {
        T a;
        if (!o.getClass().equals(o.getClass())) {
            return false;
        }
        return containsB((T)o);
    }

    @Override
    public Iterator<T> iterator() {
        return lst.iterator();
    }

    @Override
    public Object[] toArray() {
        return lst.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return lst.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        @SuppressWarnings("element-type-mismatch")
        boolean ret = lst.remove(o);
        if (ret) {
            Collections.sort(lst, cmp);
        }
        return ret;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return lst.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return add(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        boolean ret = lst.addAll(index, c);
        if (ret) {
            Collections.sort(lst, cmp);
        }
        return ret;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean ret = lst.removeAll(c);
        if (ret) {
            Collections.sort(lst, cmp);
        }
        return ret;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean ret = lst.retainAll(c);
        if (ret) {
            Collections.sort(lst, cmp);
        }
        return ret;
    }

    @Override
    public void clear() {
        lst.clear();
    }

    @Override
    public T get(int index) {
        return lst.get(index);
    }

    @Override
    public T set(int index, T element) {
        return lst.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        lst.add(index, element);
        Collections.sort(lst, cmp);
    }

    @Override
    public T remove(int index) {
        T r = lst.remove(index);
        Collections.sort(lst, cmp);
        return r;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public int indexOf(Object o) {
        T a;
        if (!o.getClass().equals(o.getClass())) {
            return lst.indexOf(o);
        }
        a = (T)o;
        for (int i = 0; i < lst.size(); i++) {
            if (cmp.compare(a, lst.get(i)) == 0) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        return lst.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return lst.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return lst.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return lst.subList(fromIndex, toIndex);
    }

}
