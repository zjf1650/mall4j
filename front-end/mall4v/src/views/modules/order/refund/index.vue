<template>
  <div class="mod-refund">
    <el-form :inline="true" :model="dataForm" @keyup.enter.native="getDataList()">
      <el-form-item>
        <el-select v-model="dataForm.status" placeholder="退款状态" clearable>
          <el-option label="全部" value=""></el-option>
          <el-option label="待审核" :value="1"></el-option>
          <el-option label="已同意" :value="2"></el-option>
          <el-option label="已拒绝" :value="3"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="getDataList()">查询</el-button>
        <el-button v-if="isAuth('admin:refund:page')" type="primary" @click="getDataList()">刷新</el-button>
      </el-form-item>
    </el-form>
    <el-table
      :data="dataList"
      border
      v-loading="dataListLoading"
      @selection-change="selectionChangeHandle"
      style="width: 100%;">
      <el-table-column
        type="selection"
        header-align="center"
        align="center"
        width="50">
      </el-table-column>
      <el-table-column
        prop="refundId"
        header-align="center"
        align="center"
        label="退款ID">
      </el-table-column>
      <el-table-column
        prop="orderNumber"
        header-align="center"
        align="center"
        label="订单号">
      </el-table-column>
      <el-table-column
        prop="refundAmount"
        header-align="center"
        align="center"
        label="退款金额">
      </el-table-column>
      <el-table-column
        prop="buyerMsg"
        header-align="center"
        align="center"
        label="申请原因">
      </el-table-column>
      <el-table-column
        prop="refundSts"
        header-align="center"
        align="center"
        label="退款状态">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.refundSts === 1" type="warning">待审核</el-tag>
          <el-tag v-else-if="scope.row.refundSts === 2" type="success">已同意</el-tag>
          <el-tag v-else-if="scope.row.refundSts === 3" type="danger">已拒绝</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="returnMoneySts"
        header-align="center"
        align="center"
        label="退款处理">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.returnMoneySts === 0" type="info">处理中</el-tag>
          <el-tag v-else-if="scope.row.returnMoneySts === 1" type="success">成功</el-tag>
          <el-tag v-else-if="scope.row.returnMoneySts === -1" type="danger">失败</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="applyTime"
        header-align="center"
        align="center"
        label="申请时间">
      </el-table-column>
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        width="150"
        label="操作">
        <template slot-scope="scope">
          <el-button v-if="isAuth('admin:refund:info')" type="text" size="small" @click="viewHandle(scope.row.refundId)">查看</el-button>
          <el-button v-if="isAuth('admin:refund:audit') && scope.row.refundSts === 1" type="text" size="small" @click="auditHandle(scope.row.refundId, 2)">同意</el-button>
          <el-button v-if="isAuth('admin:refund:audit') && scope.row.refundSts === 1" type="text" size="small" @click="auditHandle(scope.row.refundId, 3)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pageSize"
      :total="totalPage"
      layout="total, sizes, prev, pager, next, jumper">
    </el-pagination>
  </div>
</template>

<script>
export default {
  data () {
    return {
      dataForm: {
        status: ''
      },
      dataList: [],
      pageIndex: 1,
      pageSize: 10,
      totalPage: 0,
      dataListLoading: false,
      dataListSelections: []
    }
  },
  activated () {
    this.getDataList()
  },
  methods: {
    // 获取数据列表
    getDataList () {
      this.dataListLoading = true
      this.$http({
        url: this.$http.adornUrl('/admin/refund/list'),
        method: 'get',
        params: this.$http.adornParams({
          'current': this.pageIndex,
          'size': this.pageSize,
          'status': this.dataForm.status
        })
      }).then(({data}) => {
        if (data && data.code === '00000') {
          this.dataList = data.data.records
          this.totalPage = data.data.total
        } else {
          this.dataList = []
          this.totalPage = 0
        }
        this.dataListLoading = false
      })
    },
    // 每页数
    sizeChangeHandle (val) {
      this.pageSize = val
      this.pageIndex = 1
      this.getDataList()
    },
    // 当前页
    currentChangeHandle (val) {
      this.pageIndex = val
      this.getDataList()
    },
    // 多选
    selectionChangeHandle (val) {
      this.dataListSelections = val
    },
    // 查看详情
    viewHandle (refundId) {
      this.$http({
        url: this.$http.adornUrl(`/admin/refund/detail/${refundId}`),
        method: 'get'
      }).then(({data}) => {
        if (data && data.code === '00000') {
          this.$alert(`
            <div>退款ID: ${data.data.refundId}</div>
            <div>订单号: ${data.data.orderNumber}</div>
            <div>退款金额: ${data.data.refundAmount}</div>
            <div>申请原因: ${data.data.buyerMsg}</div>
            <div>申请时间: ${data.data.applyTime}</div>
          `, '退款详情', {
            dangerouslyUseHTMLString: true
          })
        }
      })
    },
    // 审核退款
    auditHandle (refundId, auditResult) {
      const auditText = auditResult === 2 ? '同意' : '拒绝'
      this.$confirm(`确定${auditText}该退款申请?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$http({
          url: this.$http.adornUrl('/admin/refund/audit'),
          method: 'put',
          data: this.$http.adornData({
            refundId: refundId,
            auditResult: auditResult,
            sellerMsg: auditResult === 2 ? '同意退款' : '拒绝退款'
          })
        }).then(({data}) => {
          if (data && data.code === '00000') {
            this.$message({
              message: '操作成功',
              type: 'success',
              duration: 1500,
              onClose: () => {
                this.getDataList()
              }
            })
          } else {
            this.$message.error(data.msg)
          }
        })
      })
    }
  }
}
</script>