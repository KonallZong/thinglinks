package com.mqttsnet.thinglinks.rule.controller;

import com.mqttsnet.thinglinks.common.core.domain.R;
import com.mqttsnet.thinglinks.common.core.enums.ConditionTypeEnum;
import com.mqttsnet.thinglinks.common.core.enums.FieldTypeEnum;
import com.mqttsnet.thinglinks.common.core.enums.OperatorEnum;
import com.mqttsnet.thinglinks.common.core.enums.TriggeringEnum;
import com.mqttsnet.thinglinks.common.core.utils.CompareUtil;
import com.mqttsnet.thinglinks.common.core.web.controller.BaseController;
import com.mqttsnet.thinglinks.common.core.web.domain.AjaxResult;
import com.mqttsnet.thinglinks.link.api.RemoteDeviceService;
import com.mqttsnet.thinglinks.link.api.RemoteProductService;
import com.mqttsnet.thinglinks.link.api.domain.product.entity.Product;
import com.mqttsnet.thinglinks.link.api.domain.product.entity.ProductProperties;
import com.mqttsnet.thinglinks.link.api.domain.product.entity.ProductServices;
import com.mqttsnet.thinglinks.rule.api.domain.Rule;
import com.mqttsnet.thinglinks.rule.api.domain.RuleConditions;
import com.mqttsnet.thinglinks.rule.service.RuleConditionsService;
import com.mqttsnet.thinglinks.rule.service.RuleService;
import com.mqttsnet.thinglinks.tdengine.api.RemoteTdEngineService;
import com.mqttsnet.thinglinks.tdengine.api.domain.TagsSelectDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.mqttsnet.thinglinks.common.core.utils.CompareUtil.rangeInDefinedInt;

/**
 * 规则处理类
 *
 * @author shisen
 */
@RestController
@RequestMapping("/rule")
public class RuleController extends BaseController {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private RuleConditionsService ruleConditionsService;

    @Autowired
    private RemoteProductService remoteProductService;

    @Autowired
    private RemoteTdEngineService remoteTdEngineService;

    @Autowired
    private RemoteDeviceService remoteDeviceService;

    /**
     * 规则触发条件验证
     */
    @GetMapping(value = "/checkRuleConditions")
    public R<?> checkRuleConditions(@RequestParam(value = "ruleIdentification", required = true) String ruleIdentification) {
        // 查询规则
        Rule rule = ruleService.selectByRuleIdentification(ruleIdentification);
        if (Objects.isNull(rule)) {
            return R.fail("规则不存在");
        }
        // 查询触发条件
        List<RuleConditions> ruleConditions = ruleConditionsService.selectByRuleId(rule.getId());
        // 存放比较结果
        List<Boolean> flags = new ArrayList<>();
        for (RuleConditions conditions : ruleConditions) {
            // 获取属性字段和类型，和设备上报的数据进行比对
            AjaxResult propertiesData = remoteProductService.selectByIdProperties(conditions.getPropertiesId());
            ProductProperties productProperties = (ProductProperties) propertiesData.get("data");
            // 获取该产品下的所有设备数据
            Map<String, Map<String, Object>> maps = extractedDeviceData(conditions);
            // 属性名称
            String productPropertiesName = productProperties.getName();
            // 属性类型
            String productPropertiesType = productProperties.getDatatype();
            // 比较模式
            String comparisonMode = conditions.getComparisonMode();
            // 比较值
            String comparisonValue = conditions.getComparisonValue();
            //  条件类型(0:匹配设备触发、1:指定设备触发、2:按策略定时触发)
            List<String> deviceDatas = new ArrayList<>();
            switch (ConditionTypeEnum.getBySymbol(conditions.getConditionType())) {
                case MATCH:
                    R<?> deviceResponse = remoteDeviceService.selectByProductIdentification(conditions.getProductIdentification());
                    if (deviceResponse != null && deviceResponse.getData() != null) {
                        deviceDatas = (List<String>) deviceResponse.getData();
                        deviceDatas.stream().forEach(s -> {
                            if (maps.containsKey(s)) {
                                Map<String, Object> stringObjectMap = maps.get(s);
                                if (stringObjectMap.containsKey(productPropertiesName)) {
                                    // 获取属性实际的值
                                    flags.add(compare(comparisonMode, productPropertiesType, stringObjectMap.get(productPropertiesName).toString(), comparisonValue));
                                }
                            }
                        });
                    }
                    break;
                case SPECIFY:
                    deviceDatas = Arrays.asList(conditions.getDeviceIdentification().split(","));
                    deviceDatas.stream().forEach(s -> {
                        if (maps.containsKey(s)) {
                            Map<String, Object> stringObjectMap = maps.get(s);
                            if (stringObjectMap.containsKey(productPropertiesName)) {
                                // 获取属性实际的值
                                flags.add(compare(comparisonMode, productPropertiesType, stringObjectMap.get(productPropertiesName).toString(), comparisonValue));
                            }
                        }
                    });
                    break;
                case STRATEGY:
                    break;
                default:
                    break;
            }
        }
        boolean mark = false;
        if (flags != null && flags.size() != 0) {
            switch (TriggeringEnum.getBySymbol(rule.getTriggering())) {
                case ALL:
                    mark = flags.stream().allMatch(s -> s.equals(true));
                    break;
                case ANY:
                    mark = flags.stream().anyMatch(s -> s.equals(true));
                    break;
                default:
                    break;
            }
        }
        // 验证条件
        return R.ok(mark);
    }

    /**
     * 获取设备上报数据
     *
     * @param conditions
     * @return
     */
    private Map<String, Map<String, Object>> extractedDeviceData(RuleConditions conditions) {
        Map<String, Map<String, Object>> maps = new HashMap<>();
        // 获取产品信息
        R productResponse = remoteProductService.selectByProductIdentification(conditions.getProductIdentification());
        Product product = (Product) productResponse.getData();
        // 获取服务信息
        AjaxResult ProductServicesResponse = remoteProductService.selectProductServicesById(conditions.getServiceId());
        ProductServices productServices = (ProductServices) ProductServicesResponse.get("data");
        // 获取超级表名称 产品类型+产品标识+服务名称
        String superName = product.getProductType() + "_" + conditions.getProductIdentification() + "_" + productServices.getServiceName();
        // 查询最新的设备记录
        TagsSelectDao tagsSelectDao = new TagsSelectDao();
        tagsSelectDao.setDataBaseName("thinglinks");
        tagsSelectDao.setStableName(superName);
        R<?> lastDataByTags = remoteTdEngineService.getLastDataByTags(tagsSelectDao);
        if (lastDataByTags != null && lastDataByTags.getData() != null) {
            maps = (Map<String, Map<String, Object>>) lastDataByTags.getData();
        }
        return maps;
    }


    /**
     * 实现计算
     *
     * @param symbol          运算符
     * @param propertiesType  属性类型
     * @param actualValue     实际值
     * @param comparisonValue 比较值
     * @return
     */
    private boolean compare(String symbol, String propertiesType, String actualValue, String comparisonValue) {
        boolean flag = false;
        // 判断比较类型
        switch (OperatorEnum.getBySymbol(symbol)) {
            case eq:
                // 判断属性值类型
                switch (FieldTypeEnum.getBySymbol(propertiesType)) {
                    case INT:
                        flag = Integer.parseInt(actualValue) == Integer.parseInt(comparisonValue);
                        break;
                    case STRING:
                        flag = actualValue.equals(comparisonValue);
                        break;
                    case DECIMAL:
                        flag = Double.parseDouble(actualValue) == Double.parseDouble(comparisonValue);
                        break;
                    case TIMESTAMP:
                        break;
                    case BOOL:
                        break;
                    default:
                        break;
                }
                break;
            case not:
                // 判断属性值类型
                switch (FieldTypeEnum.getBySymbol(propertiesType)) {
                    case INT:
                        flag = Integer.parseInt(actualValue) != Integer.parseInt(comparisonValue);
                        break;
                    case STRING:
                        flag = !actualValue.equals(comparisonValue);
                        break;
                    case DECIMAL:
                        flag = Double.parseDouble(actualValue) != Double.parseDouble(comparisonValue);
                        break;
                    case TIMESTAMP:
                        break;
                    case BOOL:
                        break;
                    default:
                        break;
                }
                break;
            case gt:
                // 判断属性值类型
                switch (FieldTypeEnum.getBySymbol(propertiesType)) {
                    case INT:
                        flag = Integer.parseInt(actualValue) > Integer.parseInt(comparisonValue);
                        break;
                    case DECIMAL:
                        flag = Double.parseDouble(actualValue) > Double.parseDouble(comparisonValue);
                        break;
                    case TIMESTAMP:
                        break;
                    case BOOL:
                        break;
                    default:
                        break;
                }
                break;
            case lt:
                // 判断属性值类型
                switch (FieldTypeEnum.getBySymbol(propertiesType)) {
                    case INT:
                        flag = Integer.parseInt(actualValue) < Integer.parseInt(comparisonValue);
                        break;
                    case DECIMAL:
                        flag = Double.parseDouble(actualValue) < Double.parseDouble(comparisonValue);
                        break;
                    case TIMESTAMP:
                        break;
                    case BOOL:
                        break;
                    default:
                        break;
                }
                break;
            case gte:
                // 判断属性值类型
                switch (FieldTypeEnum.getBySymbol(propertiesType)) {
                    case INT:
                        flag = Integer.parseInt(actualValue) >= Integer.parseInt(comparisonValue);
                        break;
                    case DECIMAL:
                        flag = Double.parseDouble(actualValue) >= Double.parseDouble(comparisonValue);
                        break;
                    case TIMESTAMP:
                        break;
                    case BOOL:
                        break;
                    default:
                        break;
                }
                break;
            case lte:
                // 判断属性值类型
                switch (FieldTypeEnum.getBySymbol(propertiesType)) {
                    case INT:
                        flag = Integer.parseInt(actualValue) <= Integer.parseInt(comparisonValue);
                        break;
                    case DECIMAL:
                        flag = Double.parseDouble(actualValue) <= Double.parseDouble(comparisonValue);
                        break;
                    case TIMESTAMP:
                        break;
                    case BOOL:
                        break;
                    default:
                        break;
                }
                break;
            case between:
                // 判断属性值类型
                switch (FieldTypeEnum.getBySymbol(propertiesType)) {
                    case INT:
                        int[] arrayint = Arrays.asList(comparisonValue.split(",")).stream().mapToInt(Integer::parseInt).toArray();
                        flag = CompareUtil.rangeInDefinedInt(Integer.parseInt(actualValue), arrayint[0], arrayint[1]);
                        break;
                    case DECIMAL:
                        double[] arrayDouble = Arrays.asList(comparisonValue.split(",")).stream().mapToDouble(Double::parseDouble).toArray();
                        flag = CompareUtil.rangeInDefinedDouble(Double.parseDouble(actualValue), arrayDouble[0], arrayDouble[1]);
                        break;
                    case TIMESTAMP:
                        break;
                    case BOOL:
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return flag;
    }


    public static void main(String[] args) {
        List<Boolean> list = new ArrayList<>();
        list.add(true);
        list.add(false);
        boolean b = list.stream().allMatch(s -> s.equals(true));
        boolean b1 = list.stream().anyMatch(s -> s.equals(true));
        System.out.println(b);
        System.out.println(b1);
    }

}
