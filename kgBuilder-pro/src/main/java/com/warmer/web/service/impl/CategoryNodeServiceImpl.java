package com.warmer.web.service.impl;

import com.warmer.web.dao.CategoryNodeDao;
import com.warmer.web.entity.CategoryNode;
import com.warmer.web.model.TreeNode;
import com.warmer.web.request.CategoryNodeQuery;
import com.warmer.web.service.CategoryNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CategoryNodeServiceImpl implements CategoryNodeService {
    @Autowired
    CategoryNodeDao categoryNodeRepository;
    @Override
    public int deleteByPrimaryKey(Integer id) {
        return categoryNodeRepository.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(CategoryNode record) {
        return categoryNodeRepository.insert(record);
    }
    @Override
    public int batchInsert(List<CategoryNode> records) {
        return categoryNodeRepository.batchInsert(records);
    }

    @Override
    public void batchUpdateExpression(List<CategoryNode> records) {
        categoryNodeRepository.batchUpdateExpression(records);
    }

    @Override
    public CategoryNode selectByPrimaryKey(Integer id) {
        return categoryNodeRepository.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKey(CategoryNode record) {
        return categoryNodeRepository.updateByPrimaryKey(record);
    }

    @Override
    public int updateCodeByPrimaryKey(Integer categoryNodeId, String systemCode) {
        return categoryNodeRepository.updateCodeByPrimaryKey(categoryNodeId, systemCode);
    }

    @Override
    public void initSystemCode(Long categoryId, String fileUuid) {
        categoryNodeRepository.initSystemCode(categoryId,fileUuid);
    }

    @Override
    public void updateNodeRelation(Long categoryId, String fileUuid, Integer categoryNodeId) {
        categoryNodeRepository.updateNodeRelation(categoryId,fileUuid,categoryNodeId);
    }

    @Override
    public void updateSystemCodeFullPath(Long categoryId, String fileUuid) {
        List<CategoryNode> nodes = categoryNodeRepository.selectByCategoryId(categoryId);
        Map<Integer, CategoryNode> nodesById = new HashMap<>();
        for (CategoryNode node : nodes) {
            nodesById.put(node.getCategoryNodeId(), node);
        }

        Map<Integer, String> paths = new HashMap<>();
        for (CategoryNode node : nodes) {
            String path = resolveSystemCode(node, nodesById, paths, new HashSet<Integer>());
            categoryNodeRepository.updateCodeByPrimaryKey(node.getCategoryNodeId(), path);
        }
    }

    @Override
    public void updateTreeLevel(Long categoryId) {
        categoryNodeRepository.updateTreeLevel(categoryId);
    }

    @Override
    public int updateLeafStatusByPrimaryKey(Integer categoryNodeId, Integer isLeaf) {
        return categoryNodeRepository.updateLeafStatusByPrimaryKey(categoryNodeId, isLeaf);
    }

    @Override
    public int reName(Integer categoryNodeId, String categoryNodeName) {
        return categoryNodeRepository.reName(categoryNodeId, categoryNodeName);
    }


    @Override
    public int deleteNodeByFileUuid(String fileUuid,String systemCode) {
        //删除与本身节点来自同一个文件的所有子节点
        return categoryNodeRepository.deleteNodeByFileUuid(fileUuid,systemCode);
    }

    @Override
    public int deleteNodeBySystemLeftRegular(String systemCode) {
        return categoryNodeRepository.deleteNodeBySystemLeftRegular(systemCode);
    }

    @Override
    public List<CategoryNode> selectByFileUuid(String fileUuid) {
        return categoryNodeRepository.selectByFileUuid(fileUuid);
    }

    @Override
    public List<CategoryNode> queryForList(CategoryNodeQuery queryItem) {
        return categoryNodeRepository.queryForList(queryItem);
    }

    @Override
    public List<TreeNode> selectByParentId(Long categoryId, Integer parentId) {
        List<CategoryNode> nodeData = categoryNodeRepository.selectByParentId(categoryId, parentId);
        List<TreeNode> item = new ArrayList<>();
        for (CategoryNode cate : nodeData) {
            TreeNode cateModel = new TreeNode();
            cateModel.setId(cate.getCategoryNodeId());
            cateModel.setLabel(cate.getCategoryNodeName());
            cateModel.setParentId(cate.getParentId());
            cateModel.setTreeLevel(cateModel.getTreeLevel());
            //添加额外的属性
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("categoryId", cate.getCategoryId());
            attributes.put("categoryNodeCode", cate.getCategoryNodeCode());
            attributes.put("systemCode", cate.getSystemCode());
            cateModel.setAttributes(attributes);
            cateModel.setLeaf(cate.getIsLeaf() == 1);
            item.add(cateModel);
        }
        return item;
    }

    @Override
    public List<CategoryNode> selectByParentIdAndName(Long categoryId, Integer parentId, String categoryNodeName) {
        return categoryNodeRepository.selectByParentIdAndName(categoryId,parentId,categoryNodeName);
    }

    @Override
    public List<CategoryNode> queryForTree(Long categoryId, Integer categoryNodeId) {
        List<CategoryNode> nodes = categoryNodeRepository.selectByCategoryId(categoryId);
        Map<Integer, List<CategoryNode>> childrenByParent = new HashMap<>();
        for (CategoryNode node : nodes) {
            Integer parentId = node.getParentId();
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<CategoryNode>()).add(node);
        }

        List<CategoryNode> result = new ArrayList<>();
        Deque<Integer> pending = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        pending.add(categoryNodeId);
        while (!pending.isEmpty()) {
            Integer parentId = pending.removeFirst();
            if (!visited.add(parentId)) {
                continue;
            }
            List<CategoryNode> children = childrenByParent.get(parentId);
            if (children == null) {
                continue;
            }
            result.addAll(children);
            for (CategoryNode child : children) {
                pending.addLast(child.getCategoryNodeId());
            }
        }
        result.sort(Comparator.comparing(CategoryNode::getTreeLevel, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(CategoryNode::getCategoryNodeId));
        return result;
    }

    @Override
    public List<CategoryNode> selectTreeForParent(Integer categoryNodeId) {
        return collectParentChain(categoryNodeRepository.selectByPrimaryKey(categoryNodeId));
    }

    @Override
    public List<CategoryNode> selectTreeForParentBySystemCode(String systemCode) {
        return collectParentChain(categoryNodeRepository.selectBySystemCode(systemCode));
    }

    @Override
    public List<CategoryNode> selectRecentEditNode(Long categoryId) {
        return categoryNodeRepository.selectRecentEditNode(categoryId);
    }

    @Override
    public List<TreeNode> getTreeData(Long categoryId, Integer categoryNodeId) {
        List<CategoryNode> CategoryNodes = queryForTree(categoryId, categoryNodeId);
        return getTree(categoryNodeId, CategoryNodes);
    }

    private String resolveSystemCode(CategoryNode node, Map<Integer, CategoryNode> nodesById,
                                     Map<Integer, String> paths, Set<Integer> visiting) {
        Integer nodeId = node.getCategoryNodeId();
        String cached = paths.get(nodeId);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(nodeId)) {
            return String.valueOf(nodeId);
        }

        String path = String.valueOf(nodeId);
        Integer parentId = node.getParentId();
        CategoryNode parent = parentId == null ? null : nodesById.get(parentId);
        if (parentId != null && parentId != 0 && parent != null) {
            path = resolveSystemCode(parent, nodesById, paths, visiting) + "/" + nodeId;
        }
        visiting.remove(nodeId);
        paths.put(nodeId, path);
        return path;
    }

    private List<CategoryNode> collectParentChain(CategoryNode start) {
        List<CategoryNode> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        CategoryNode current = start;
        while (current != null && current.getCategoryNodeId() != null
                && visited.add(current.getCategoryNodeId())) {
            result.add(current);
            Integer parentId = current.getParentId();
            current = parentId == null || parentId == 0
                    ? null
                    : categoryNodeRepository.selectByPrimaryKey(parentId);
        }
        result.sort(Comparator.comparing(CategoryNode::getTreeLevel, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(CategoryNode::getCategoryNodeId));
        return result;
    }

    private List<TreeNode> getTree(int parentId, List<CategoryNode> nodeList) {
        List<TreeNode> item = new ArrayList<>();
        Iterator<CategoryNode> treeList = nodeList.stream().filter(m -> m.getParentId() == parentId).iterator();
        while (treeList.hasNext()) {
            CategoryNode cate = treeList.next();
            TreeNode cateModel = new TreeNode();
            cateModel.setId(cate.getCategoryNodeId());
            cateModel.setLabel(cate.getCategoryNodeName());
            cateModel.setParentId(cate.getParentId());
            cateModel.setTreeLevel(cateModel.getTreeLevel());
            //添加额外的属性
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("categoryId", cate.getCategoryId());
            attributes.put("categoryNodeCode", cate.getCategoryNodeCode());
            cateModel.setAttributes(attributes);
            List<TreeNode> childrenList = getTree(cate.getCategoryNodeId(), nodeList);
            if (!childrenList.isEmpty()) {
                cateModel.setChildren(childrenList);
            }
            cateModel.setLeaf(childrenList.isEmpty());
            item.add(cateModel);
        }
        return item;
    }

}
