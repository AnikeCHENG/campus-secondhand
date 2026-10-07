package com.example.campussecondhand.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.common.PageResult;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 大厅物理分页测试。
 *
 * <p>核心是证明「筛选 + 排序 + 分页在同一条 SQL 内完成」：
 * 分页后若把筛选留在前端，结果只会覆盖当前页，这是必须防回归的点。
 * 另外 {@code condition} 列是 MySQL 保留字，漏掉反引号会直接语法错误。</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductPageQueryTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    /** ProductService 用字段注入，没有构造器，只能靠 @InjectMocks */
    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        // @InjectMocks 在 Mockito 扩展初始化时注入，此处无需手工装配
    }

    private IPage<Product> emptyPage() {
        return new Page<>(1, 10, 0);
    }

/**
     * 捕获传给 Mapper 的查询条件。
     *
     * <p>捕获成 {@link QueryWrapper} 而非 {@code Wrapper}：绑定值只能从
     * {@code getParamNameValuePairs()} 取（{@code getTargetSql()} 里是 {@code #{}} 占位符），
     * 而该方法不在 {@code Wrapper} 接口上。</p>
     */
    private QueryWrapper<Product> captureWrapper() {
        ArgumentCaptor<Wrapper<Product>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(productRepository).selectPage(any(), captor.capture());
        QueryWrapper<Product> wrapper = (QueryWrapper<Product>) captor.getValue();
        // 必须先渲染一次 SQL，paramNameValuePairs 才会有绑定值；
        // 直接读它是空 Map，会让人误以为条件没生效
        wrapper.getTargetSql();
        return wrapper;
    }

    @Test
    @DisplayName("只查在售商品，并按创建时间倒序")
    void filtersOnSaleAndSortsByNewest() {
        when(productRepository.selectPage(any(), any())).thenReturn(emptyPage());

        productService.pageAvailable(PageParam.of(1, 10), null, null, null, null, null, "newest");

        String sql = captureWrapper().getTargetSql().replaceAll("\\s+", " ").trim();
        assertThat(sql).contains("status =");
        assertThat(sql).contains("ORDER BY created_time DESC");
    }

    @Test
    @DisplayName("分类与关键词下推到 SQL 的 WHERE 条件")
    void pushesCategoryAndKeywordIntoWhere() {
        when(productRepository.selectPage(any(), any())).thenReturn(emptyPage());

        productService.pageAvailable(PageParam.of(1, 10), "books", "教材",
                null, null, null, "newest");

        String sql = captureWrapper().getTargetSql().replaceAll("\\s+", " ").trim();
        assertThat(sql).contains("category =");
        assertThat(sql).contains("title LIKE");
        assertThat(sql).contains("description LIKE");
        // OR 必须被 and(...) 包住，否则会泄漏到价格等后续条件之外
        assertThat(sql).contains("AND (");
    }

    @Test
    @DisplayName("价格区间下推到 SQL")
    void pushesPriceRangeIntoWhere() {
        when(productRepository.selectPage(any(), any())).thenReturn(emptyPage());

        productService.pageAvailable(PageParam.of(1, 10), null, null,
                new BigDecimal("10.00"), new BigDecimal("99.00"), null, "newest");

        String sql = captureWrapper().getTargetSql().replaceAll("\\s+", " ").trim();
        assertThat(sql).contains("price >=");
        assertThat(sql).contains("price <=");
    }

    @Test
    @DisplayName("condition 是 MySQL 保留字，必须带反引号")
    void quotesReservedWordCondition() {
        when(productRepository.selectPage(any(), any())).thenReturn(emptyPage());

        productService.pageAvailable(PageParam.of(1, 10), null, null,
                null, null, "9成新", "newest");

        String sql = captureWrapper().getTargetSql().replaceAll("\\s+", " ").trim();
        assertThat(sql).contains("`condition` =");
    }

    @Test
    @DisplayName("四种排序分别映射到正确的 ORDER BY")
    void mapsSortKeys() {
        assertThat(orderBy("priceAsc")).contains("price ASC");
        assertThat(orderBy("priceDesc")).contains("price DESC");
        assertThat(orderBy("hot")).contains("view_count DESC");
        assertThat(orderBy("newest")).contains("created_time DESC");
        // 未知排序值必须退回默认，而不是抛异常或产生非法 SQL
        assertThat(orderBy("hacker")).contains("created_time DESC");
        assertThat(orderBy(null)).contains("created_time DESC");
    }

private String orderBy(String sort) {
        // 同一测试内会多次调用，先清掉上一次的调用记录，否则 verify 次数会累积
        org.mockito.Mockito.clearInvocations(productRepository);
        when(productRepository.selectPage(any(), any())).thenReturn(emptyPage());
        productService.pageAvailable(PageParam.of(1, 10), null, null, null, null, null, sort);
        return captureWrapper().getTargetSql().replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("分页对象带着 current/size 交给 MyBatis-Plus，由数据库截断")
    void passesPagingToMapper() {
        when(productRepository.selectPage(any(), any())).thenReturn(new Page<>(4, 25, 130));

        IPage<Product> result =
                productService.pageAvailable(PageParam.of(4, 25), null, null, null, null, null, "newest");

        ArgumentCaptor<IPage> captor = ArgumentCaptor.forClass(IPage.class);
        verify(productRepository).selectPage(captor.capture(), any());
        assertThat(captor.getValue().getCurrent()).isEqualTo(4);
        assertThat(captor.getValue().getSize()).isEqualTo(25);
        assertThat(result.getTotal()).isEqualTo(130);
    }

    @Test
    @DisplayName("非法分页参数在到达数据库前就被拒绝")
    void rejectsInvalidPagingBeforeQuerying() {
        assertThatThrownBy(() -> PageParam.of(0, 10)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> PageParam.of(1, 101)).isInstanceOf(BadRequestException.class);

        // 校验发生在 service 调用之前，因此不会产生任何数据库查询
        verify(productRepository, org.mockito.Mockito.never()).selectPage(any(), any());
    }

@Test
    @DisplayName("统一返回结构为 list/total/page/size")
    void pageResultExposesUnifiedShape() {
        List<Product> records = List.of(new Product(), new Product());
        Page<Product> paged = new Page<>(2, 10, 31);
        paged.setRecords(records);

        PageResult<Product> result = PageResult.of(paged);

        assertThat(result.list()).isEqualTo(records);
        assertThat(result.total()).isEqualTo(31);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        // 字段名即接口契约：list / total / page / size
        assertThat(PageResult.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("list", "total", "page", "size");
    }

    @Test
    @DisplayName("在售过滤用的是枚举常量而非魔法数字")
    void usesEnumConstantForOnSaleStatus() {
        when(productRepository.selectPage(any(), any())).thenReturn(emptyPage());

        productService.pageAvailable(PageParam.of(1, 10), null, null, null, null, null, "newest");

        // getTargetSql 里是 #{} 占位符，绑定值要去 paramNameValuePairs 里找
        assertThat(captureWrapper().getParamNameValuePairs())
                .containsValue(ProductStatus.ON_SALE.getCode());
    }
}