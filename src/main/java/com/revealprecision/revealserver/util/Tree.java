package com.revealprecision.revealserver.util;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class Tree<K, T> {

  private LinkedHashMap<K, TreeNode<K, T>> map;
  private LinkedHashMap<K, LinkedHashSet<K>> parentChildren;

  public LinkedHashMap<K, TreeNode<K, T>> getTree() {
    return map;
  }

  public LinkedHashMap<K, LinkedHashSet<K>> getChildParent() {
    return parentChildren;
  }

  public Tree() {
    map = new LinkedHashMap<K, TreeNode<K, T>>();
    parentChildren = new LinkedHashMap<>();
  }

  private void addToParentChildRelation(K parent, K id) {
    if (parentChildren == null) {
      parentChildren = new LinkedHashMap<>();
    }

    LinkedHashSet<K> kids = parentChildren.get(parent);
    if (kids == null) {
      kids = new LinkedHashSet<>();
    }

    kids.add(id);
    parentChildren.put(parent, kids);
  }

  public void addNode(K id, String label, T node, K parentId) {
    if (map == null) {
      map = new LinkedHashMap<>();
    }

    if (hasNode(id)) {
      throw new IllegalArgumentException("Node with ID " + id + " already exists in tree");
    }

    TreeNode<K, T> n = makeNode(id, label, node, parentId);

    if (parentId != null) {
      addToParentChildRelation(parentId, id);

      TreeNode<K, T> p = getNode(parentId);

      if (p != null) {
        p.addChild(n);
      } else {
        map.put(id, n);
      }
    } else {
      map.put(id, n);
    }

    LinkedHashSet<K> kids = parentChildren.get(id);
    if (kids != null) {
      for (K kid : kids) {
        TreeNode<K, T> kn = removeNode(kid);
        n.addChild(kn);
      }
    }
  }

  private TreeNode<K, T> makeNode(K id, String label, T node, K parentId) {
    TreeNode<K, T> n = getNode(id);
    if (n == null) {
      n = new TreeNode<K, T>(id, label, node, parentId);
    }
    return n;
  }

  public TreeNode<K, T> getNode(K id) {
    // Check if id is any root node
    if (map.containsKey(id)) {
      return map.get(id);
    }

    // neither root itself nor parent of root
    for (TreeNode<K, T> root : map.values()) {
      TreeNode<K, T> n = root.findChild(id);
      if (n != null)
        return n;
    }
    return null;
  }

  private TreeNode<K, T> removeNode(K id) {
    // Check if id is any root node
    if (map.containsKey(id)) {
      return map.remove(id);
    }
    // neither root itself nor parent of root
    for (TreeNode<K, T> root : map.values()) {
      TreeNode<K, T> n = root.removeChild(id);
      if (n != null) {
        return n;
      }
    }
    return null;
  }

  /**
   * Delete nodes from location hierarchy
   *
   * @param id the id of the node to remove
   */
  public void deleteNode(K id) {
    TreeNode<K, T> node = getNode(id);
    if (node == null)
      return;
    removeNode(id);
    parentChildren.remove(id);
    LinkedHashSet<K> parent = parentChildren.get(node.getParent());
    if (parent != null && parent.size() == 1) {
      deleteNode(node.getParent());
    } else if (parent != null) {
      parent.remove(id);
    }
  }


  public boolean hasNode(K id) {
    return getNode(id) != null;
  }
}