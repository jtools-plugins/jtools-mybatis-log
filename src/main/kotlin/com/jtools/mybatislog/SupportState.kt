package com.jtools.mybatislog

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service
@State(name = "data", storages = [Storage("JToolsMybatisPluginLogState.xml")])
class SupportState {


    private var state = State()


    companion object {
//        var supportState = SupportState()
        fun getInstance(project: Project) = project.getService(SupportState::class.java).state
//        fun getInstance(project: Project) = supportState.state

    }

    class State {
        var supportClasses = """
            org.apache.ibatis.session.Configuration
            com.baomidou.mybatisplus.core.MybatisConfiguration
        """.trimIndent()

        var sqlFormatType = "Mysql"


        var supportSqlFormatTypes = arrayOf(
            "Mysql",
            "Db2",
            "MariaDb",
            "N1ql",
            "PlSql",
            "PostgreSql",
            "Redshift",
            "SparkSql",
            "StandardSql",
            "TSql",
        )
    }

}