package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import com.leak.intelligentcustomerchat.domain.knowledge.KnowledgeDocument;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KnowledgeSeedCatalog {

    public List<KnowledgeDocument> builtInDocuments() {
        return List.of(
                new KnowledgeDocument(
                        "seed-pre-sales-product-selection",
                        "售前选品问答",
                        """
                        当客户询问“哪一款更适合我的空间或用途”时，先确认使用场景、预算、安装位置、对亮度和色温的偏好。

                        如果客户只描述了氛围诉求而没有给出尺寸、安装方式或供电条件，应先追问这些关键信息，再给出推荐方向。

                        回复中不要承诺不存在的规格。只能基于已知资料说明适合的使用场景、安装建议和兼容范围。
                        """,
                        Map.of(
                                "scene", "PRE_SALES",
                                "subIntents", "product_recommendation,general_inquiry",
                                "source", "builtin-seed"
                        )
                ),
                new KnowledgeDocument(
                        "seed-pre-sales-comparison",
                        "售前对比说明",
                        """
                        当客户询问产品差异时，优先从安装位置、控制方式、尺寸限制、亮度范围和适用空间说明区别。

                        如果客户问题里带有“哪个更适合我”，除了对比差异，还要回到客户当前场景给出建议方向。

                        对比回复要避免绝对化描述，尤其不要把“更亮”“更好”说成结论，除非有明确参数支撑。
                        """,
                        Map.of(
                                "scene", "PRE_SALES",
                                "subIntents", "product_comparison,general_inquiry",
                                "source", "builtin-seed"
                        )
                ),
                new KnowledgeDocument(
                        "seed-pre-sales-shipping-stock",
                        "售前库存与发货咨询",
                        """
                        对库存和发货时效问题，先说明是否需要以系统实时结果为准，再给出当前可确认的信息。

                        如果没有实时库存接口，就不要承诺具体库存数量，只能说明“需要进一步核实”或“以实际下单页为准”。

                        对跨境发货时效，要区分“订单处理时间”和“物流运输时间”，避免把两者混为一个承诺。
                        """,
                        Map.of(
                                "scene", "PRE_SALES",
                                "subIntents", "inventory_or_shipping,general_inquiry",
                                "source", "builtin-seed"
                        )
                ),
                new KnowledgeDocument(
                        "seed-after-sales-logistics",
                        "售后物流状态说明",
                        """
                        当客户咨询物流状态时，应优先引用订单系统或物流系统返回的最新节点，不要凭经验猜测包裹位置。

                        如果只有“运输中”这样的宽泛状态，回复时要明确说明当前已确认的信息，以及下一次建议关注的更新时间。

                        如果缺少订单号或物流号，应先追问关键编号，再继续核查。
                        """,
                        Map.of(
                                "scene", "AFTER_SALES",
                                "subIntents", "logistics_tracking,order_status",
                                "source", "builtin-seed"
                        )
                ),
                new KnowledgeDocument(
                        "seed-after-sales-policy",
                        "售后政策说明",
                        """
                        当客户询问退货、退款、换货、保修等政策时，先确认订单事实，再引用平台或店铺的标准政策。

                        如果客户要求例外处理、额外赔偿或超出标准政策的承诺，应保留人工审核节点，不要自动答应。

                        政策回复里要清楚区分“可执行的标准流程”和“需要人工进一步确认的例外情况”。
                        """,
                        Map.of(
                                "scene", "AFTER_SALES",
                                "subIntents", "after_sales_policy,return_refund,general_inquiry",
                                "source", "builtin-seed"
                        )
                ),
                new KnowledgeDocument(
                        "seed-after-sales-multi-round",
                        "售后多轮邮件处理提示",
                        """
                        对多轮售后邮件，应优先利用线程摘要识别客户已经提供过的信息，避免重复追问同一个字段。

                        如果客户前文已经给过订单号或物流号，而本轮邮件只是在追问进度，回复时应直接基于现有事实继续推进。

                        当上下文和实时查询结果冲突时，要以最新业务系统事实为准，并在回复里说明原因。
                        """,
                        Map.of(
                                "scene", "AFTER_SALES",
                                "subIntents", "logistics_tracking,after_sales_policy,return_refund,general_inquiry",
                                "source", "builtin-seed"
                        )
                )
        );
    }
}
