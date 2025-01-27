package com.mqttsnet.thinglinks.link.mapper.product;
import java.util.Collection;

import com.mqttsnet.thinglinks.link.api.domain.product.entity.Product;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: java类作用描述
 * @Author: ShiHuan Sun
 * @E-mail: 13733918655@163.com
 * @Website: http://thinglinks.mqttsnet.com
 * @CreateDate: 2021/12/25$ 23:52$
 * @UpdateUser: ShiHuan Sun
 * @UpdateDate: 2021/12/25$ 23:52$
 * @UpdateRemark: 修改内容
 * @Version: 1.0
 */
@Mapper
public interface ProductMapper {
    /**
     * delete by primary key
     *
     * @param id primaryKey
     * @return deleteCount
     */
    int deleteByPrimaryKey(Long id);

    /**
     * insert record to table
     *
     * @param record the record
     * @return insert count
     */
    int insert(Product record);

    int insertOrUpdate(Product record);

    int insertOrUpdateSelective(Product record);

    /**
     * insert record to table selective
     *
     * @param record the record
     * @return insert count
     */
    int insertSelective(Product record);

    /**
     * select by primary key
     *
     * @param id primary key
     * @return object by primary key
     */
    Product selectByPrimaryKey(Long id);

    /**
     * update record selective
     *
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKeySelective(Product record);

    /**
     * update record
     *
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKey(Product record);

    int updateBatch(List<Product> list);

    int updateBatchSelective(List<Product> list);

    int batchInsert(@Param("list") List<Product> list);

    /**
     * 查询产品管理
     *
     * @param id 产品管理主键
     * @return 产品管理
     */
    public Product selectProductById(Long id);

    /**
     * 查询产品管理
     *
     * @param productIdentification 产品标识
     * @return 产品管理
     */
    public Product selectByProductIdentification(String productIdentification);

    /**
     * 查询产品管理列表
     *
     * @param product 产品管理
     * @return 产品管理集合
     */
    public List<Product> selectProductList(Product product);

    /**
     * 新增产品管理
     *
     * @param product 产品管理
     * @return 结果
     */
    public int insertProduct(Product product);

    /**
     * 修改产品管理
     *
     * @param product 产品管理
     * @return 结果
     */
    public int updateProduct(Product product);

    /**
     * 删除产品管理
     *
     * @param id 产品管理主键
     * @return 结果
     */
    public int deleteProductById(Long id);

    /**
     * 批量删除产品管理
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProductByIds(Long[] ids);

    Product findOneByProductName(@Param("productName") String productName);

    List<Product> selectByManufacturerIdAndModelAndDeviceType(@Param("manufacturerId")String manufacturerId,@Param("model")String model,@Param("deviceType")String deviceType);

	Product findOneByManufacturerIdAndModelAndDeviceType(@Param("manufacturerId")String manufacturerId,@Param("model")String model,@Param("deviceType")String deviceType);


    List<Product> findAllByStatus(@Param("status")String status);


    Product findOneByManufacturerIdAndModelAndProtocolTypeAndStatus(@Param("manufacturerId")String manufacturerId,@Param("model")String model,@Param("protocolType")String protocolType,@Param("status")String status);

    Product findOneByIdAndStatus(@Param("id")Long id,@Param("status")String status);

    Product findOneByProductIdentificationAndProtocolType(@Param("productIdentification")String productIdentification,@Param("protocolType")String protocolType);

    List<Product> findAllByIdInAndStatus(@Param("idCollection")Collection<Long> idCollection,@Param("status")String status);





}
