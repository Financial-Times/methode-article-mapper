# Class: methode_article_transformer
# vim: ts=4 sts=4 sw=4 et sr smartindent:
# This module manages methode_article_transformer
#
# Parameters:
#
# Actions:
#
# Requires:
#
# Sample Usage:
#
class methode_article_transformer {

    $jar_name = 'methode-article-transformer-service.jar'
    $dir_heap_dumps = "/var/log/apps/methode_article_transformer-heap-dumps"

    class { 'content_platform_nagios::client': }
    class { 'hosts::export': hostname => "$certname" }
    class { "${module_name}::monitoring": }
    class { 'sudoers_sudocont': }

    content_runnablejar { "${module_name}_runnablejar":
        service_name        => "${module_name}",
        service_description => 'Methode Article Transformer',
        jar_name            => "${jar_name}",
        artifact_location   => "${module_name}/methode-article-transformer-service.jar",
        config_file_content => template("${module_name}/config.yml.erb"),
        status_check_url    => "http://localhost:8081/ping";
    }

    file { "sysconfig":
        path    => "/etc/sysconfig/${module_name}",
        ensure  => 'present',
        content => template("${module_name}/sysconfig.erb"),
        mode    => 644;
    }

    file { "heap-dumps-dir":
        path    => "${dir_heap_dumps}",
        owner   => "${module_name}",
        group   => "${module_name}",
        ensure  => 'directory',
        mode    => 744;
    }

    File['sysconfig']
    -> Content_runnablejar["${module_name}_runnablejar"]
    -> Class["${module_name}::monitoring"]

}

